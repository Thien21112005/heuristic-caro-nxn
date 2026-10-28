package controller;

import model.EndReason;
import model.GameModel;
import net.Connection;
import net.LanClient;
import net.LanHost;
import net.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/** Two NetworkGameControllers talking over 127.0.0.1. */
class NetworkGameControllerTest {
    private NetworkGameController host;
    private NetworkGameController guest;
    private Connection hostConn;
    private Connection guestConn;
    private final Events hostEvents = new Events();
    private final Events guestEvents = new Events();

    static final class Events implements GameSession.Listener {
        volatile int restartRequests;
        volatile int declines;
        volatile String lost;
        volatile int gameOvers;

        @Override public void onStateChanged() { }
        @Override public void onGameOver() { gameOvers++; }
        @Override public void onAiThinking(boolean thinking) { }
        @Override public void onRestartRequested() { restartRequests++; }
        @Override public void onRestartDeclined() { declines++; }
        @Override public void onConnectionLost(String reason) { lost = reason; }
    }

    @BeforeEach
    void connect() throws Exception {
        LanHost server = new LanHost(InetAddress.getLoopbackAddress(), 0);
        CompletableFuture<Socket> accepted = new CompletableFuture<>();
        server.acceptAsync(accepted::complete, accepted::completeExceptionally);
        Socket client = LanClient.connect("127.0.0.1", server.getPort());
        hostConn = new Connection(accepted.get(3, TimeUnit.SECONDS));
        guestConn = new Connection(client);
        edt(() -> {
            host = new NetworkGameController(hostConn, true, 10, 0, "Guest");
            guest = new NetworkGameController(guestConn, false, 10, 0, "Host");
            host.setListener(hostEvents);
            guest.setListener(guestEvents);
            hostConn.start(host);
            guestConn.start(guest);
            host.start();
            guest.start();
        });
    }

    @AfterEach
    void close() throws Exception {
        edt(() -> {
            host.dispose();
            guest.dispose();
        });
    }

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static <T> T onEdt(java.util.concurrent.Callable<T> c) throws Exception {
        AtomicReference<T> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                ref.set(c.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return ref.get();
    }

    private static void await(String what, BooleanSupplier condition) throws Exception {
        long end = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < end) {
            AtomicReference<Boolean> ok = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> ok.set(condition.getAsBoolean()));
            if (ok.get()) return;
            Thread.sleep(20);
        }
        fail("timed out waiting for " + what);
    }

    @Test
    void hostOpensAsXAndMovesReachTheOtherSide() throws Exception {
        assertEquals(GameModel.X, (int) onEdt(host::getHumanSide));
        assertEquals(GameModel.O, (int) onEdt(guest::getHumanSide));
        assertFalse(onEdt(guest::acceptsInput), "O must wait for X");

        edt(() -> host.play(4, 4));
        await("guest to see X", () -> guest.getModel().getCell(4, 4) == GameModel.X);
        assertTrue(onEdt(guest::acceptsInput));
        assertFalse(onEdt(host::acceptsInput));

        edt(() -> host.play(0, 0)); // not the host's turn: ignored locally, nothing sent
        edt(() -> guest.play(5, 5));
        await("host to see O", () -> host.getModel().getCell(5, 5) == GameModel.O);
        assertEquals(GameModel.EMPTY, (int) onEdt(() -> guest.getModel().getCell(0, 0)));
    }

    @Test
    void aWinIsSeenOnBothMachines() throws Exception {
        for (int i = 0; i < 5; i++) {
            int col = i;
            edt(() -> host.play(0, col));
            if (i == 4) break;
            await("X at " + col, () -> guest.getModel().getCell(0, col) == GameModel.X);
            edt(() -> guest.play(5, col));
            await("O at " + col, () -> host.getModel().getCell(5, col) == GameModel.O);
        }
        await("guest to see the win", () -> guest.getModel().getWinner() == GameModel.X);
        assertEquals(GameModel.X, (int) onEdt(() -> host.getModel().getWinner()));
        assertEquals(1, hostEvents.gameOvers);
        assertEquals(1, guestEvents.gameOvers);
    }

    @Test
    void newRoundNeedsAgreementAndSwapsPieces() throws Exception {
        edt(() -> host.play(4, 4));
        await("move", () -> guest.getModel().getMoveCount() == 1);
        edt(() -> host.newRound(true));
        await("guest to be asked", () -> guestEvents.restartRequests == 1);
        assertTrue(onEdt(host::isRestartPending));
        assertFalse(onEdt(host::acceptsInput), "no moves while waiting for an answer");

        edt(() -> guest.answerRestart(true));
        await("host to start round 2", () -> host.getRound() == 2);
        assertEquals(2, (int) onEdt(guest::getRound));
        assertEquals(0, (int) onEdt(() -> host.getModel().getMoveCount()));
        assertEquals(GameModel.O, (int) onEdt(host::getHumanSide), "host plays O in even rounds");
        assertEquals(GameModel.X, (int) onEdt(guest::getHumanSide));
        assertTrue(onEdt(guest::acceptsInput), "the guest now opens");
    }

    @Test
    void declinedRestartKeepsTheCurrentGame() throws Exception {
        edt(() -> host.play(4, 4));
        await("move", () -> guest.getModel().getMoveCount() == 1);
        edt(guest::restart);
        await("host to be asked", () -> hostEvents.restartRequests == 1);
        edt(() -> host.answerRestart(false));
        await("guest to hear no", () -> guestEvents.declines == 1);
        assertFalse(onEdt(guest::isRestartPending));
        assertEquals(1, (int) onEdt(() -> guest.getModel().getMoveCount()));
        assertEquals(1, (int) onEdt(host::getRound));
    }

    @Test
    void simultaneousRestartRequestsStartExactlyOneRound() throws Exception {
        edt(() -> {
            host.restart();
            guest.restart();
        });
        await("both in round 2", () -> host.getRound() == 2 && guest.getRound() == 2);
        Thread.sleep(300);
        assertEquals(2, (int) onEdt(host::getRound));
        assertEquals(2, (int) onEdt(guest::getRound));
        assertFalse(onEdt(host::isRestartPending));
        assertFalse(onEdt(guest::isRestartPending));
    }

    @Test
    void resignationEndsTheGameForBoth() throws Exception {
        edt(guest::resign);
        await("host to see the resignation", () -> host.getModel().isOver());
        assertEquals(GameModel.X, (int) onEdt(() -> host.getModel().getWinner()));
        assertEquals(EndReason.RESIGN, onEdt(() -> host.getModel().getEndReason()));
        assertEquals(GameModel.X, (int) onEdt(() -> guest.getModel().getWinner()));
    }

    @Test
    void opponentLeavingCountsAsADisconnect() throws Exception {
        edt(() -> host.play(4, 4));
        await("move", () -> guest.getModel().getMoveCount() == 1);
        edt(guest::dispose);
        await("host to notice", () -> hostEvents.lost != null);
        assertEquals("bye", hostEvents.lost);
        assertEquals(EndReason.DISCONNECT, onEdt(() -> host.getModel().getEndReason()));
        assertEquals(GameModel.X, (int) onEdt(() -> host.getModel().getWinner()));
        assertFalse(onEdt(host::acceptsInput));
    }

    @Test
    void illegalMoveFromThePeerIsADesync() throws Exception {
        edt(() -> host.play(4, 4));
        await("move", () -> guest.getModel().getMoveCount() == 1);
        // A buggy or tampered client claims the occupied centre
        guestConn.send(new Protocol.Move(4, 4));
        await("host to drop the link", () -> hostEvents.lost != null);
        assertEquals("desync", hostEvents.lost);
        await("guest to be told", () -> guestEvents.lost != null);
    }
}
