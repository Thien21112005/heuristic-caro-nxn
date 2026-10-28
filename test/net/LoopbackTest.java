package net;

import model.GameModel;
import net.Protocol.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Host and client over 127.0.0.1 (no firewall prompt, no real network needed). */
class LoopbackTest {
    private static final long WAIT = 3;

    private final List<AutoCloseable> open = new ArrayList<>();

    /** Collects what a Connection delivers; callbacks run directly on the reader thread. */
    private static final class Probe implements Connection.Listener {
        final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
        final CompletableFuture<String> closed = new CompletableFuture<>();

        @Override
        public void onMessage(Message message) { messages.add(message); }

        @Override
        public void onClosed(String reason) { closed.complete(reason); }

        Message next() throws InterruptedException {
            Message m = messages.poll(WAIT, TimeUnit.SECONDS);
            assertNotNull(m, "no message arrived");
            return m;
        }
    }

    @AfterEach
    void closeAll() throws Exception {
        for (AutoCloseable c : open) c.close();
    }

    private Socket[] pair() throws Exception {
        LanHost host = new LanHost(InetAddress.getLoopbackAddress(), 0);
        open.add(host);
        CompletableFuture<Socket> accepted = new CompletableFuture<>();
        host.acceptAsync(accepted::complete, accepted::completeExceptionally);
        Socket client = LanClient.connect("127.0.0.1", host.getPort());
        Socket server = accepted.get(WAIT, TimeUnit.SECONDS);
        open.add(client);
        open.add(server);
        return new Socket[]{server, client};
    }

    private Connection connect(Socket s, Probe probe, long heartbeatMs, long timeoutMs) throws Exception {
        Connection c = new Connection(s, Runnable::run, heartbeatMs, timeoutMs);
        open.add(c);
        c.start(probe);
        return c;
    }

    @Test
    void playsAFullGameAndBothSidesAgree() throws Exception {
        Socket[] s = pair();
        Probe hostProbe = new Probe(), clientProbe = new Probe();
        Connection host = connect(s[0], hostProbe, 2_000, 8_000);
        Connection client = connect(s[1], clientProbe, 2_000, 8_000);

        host.send(new Hello(Protocol.VERSION, "Host", 10, 0));
        assertEquals(new Hello(Protocol.VERSION, "Host", 10, 0), clientProbe.next());
        client.send(new Hello(Protocol.VERSION, "Guest", 0, 0));
        assertEquals("Guest", ((Hello) hostProbe.next()).name());

        GameModel hostModel = new GameModel(10);
        GameModel clientModel = new GameModel(10);
        // Host plays X along row 0, the client plays O along row 5; X wins on its fifth stone
        for (int i = 0; !hostModel.isOver(); i++) {
            boolean hostTurn = i % 2 == 0;
            int row = hostTurn ? 0 : 5;
            int col = i / 2;
            if (hostTurn) {
                assertTrue(hostModel.play(row, col));
                host.send(new Move(row, col));
                Move m = (Move) clientProbe.next();
                assertTrue(clientModel.play(m.row(), m.col()));
            } else {
                assertTrue(clientModel.play(row, col));
                client.send(new Move(row, col));
                Move m = (Move) hostProbe.next();
                assertTrue(hostModel.play(m.row(), m.col()));
            }
        }
        assertEquals(GameModel.X, hostModel.getWinner());
        assertEquals(hostModel.getWinner(), clientModel.getWinner());
        assertArrayEquals(hostModel.getWinningLine(), clientModel.getWinningLine());

        client.send(new Simple(Kind.RESTART_REQ));
        assertEquals(new Simple(Kind.RESTART_REQ), hostProbe.next());
        host.send(new Simple(Kind.RESIGN));
        assertEquals(new Simple(Kind.RESIGN), clientProbe.next());
    }

    @Test
    void byeClosesBothEnds() throws Exception {
        Socket[] s = pair();
        Probe hostProbe = new Probe(), clientProbe = new Probe();
        Connection host = connect(s[0], hostProbe, 2_000, 8_000);
        connect(s[1], clientProbe, 2_000, 8_000);
        host.close("leaving");
        assertEquals(new Bye("leaving"), clientProbe.next());
        assertEquals("bye", clientProbe.closed.get(WAIT, TimeUnit.SECONDS));
        assertEquals("local", hostProbe.closed.get(WAIT, TimeUnit.SECONDS));
        assertFalse(host.isOpen());
    }

    @Test
    void silentPeerTimesOut() throws Exception {
        Socket[] s = pair();
        Probe hostProbe = new Probe();
        connect(s[0], hostProbe, 100, 400); // the raw client socket never answers PINGs
        assertEquals("timeout", hostProbe.closed.get(WAIT, TimeUnit.SECONDS));
    }

    @Test
    void heartbeatKeepsAnIdleLinkAlive() throws Exception {
        Socket[] s = pair();
        Probe hostProbe = new Probe(), clientProbe = new Probe();
        connect(s[0], hostProbe, 100, 400);
        connect(s[1], clientProbe, 100, 400);
        Thread.sleep(1_200); // three timeout windows with no game traffic
        assertFalse(hostProbe.closed.isDone(), "PING/PONG should keep the link open");
        assertFalse(clientProbe.closed.isDone());
        assertTrue(hostProbe.messages.isEmpty(), "heartbeats are not delivered to the listener");
    }

    @Test
    void garbageInputClosesTheLink() throws Exception {
        Socket[] s = pair();
        Probe hostProbe = new Probe();
        connect(s[0], hostProbe, 2_000, 8_000);
        OutputStream raw = s[1].getOutputStream();
        raw.write("FIRE ZE MISSILES\n".getBytes(StandardCharsets.UTF_8));
        raw.flush();
        assertEquals("protocol", hostProbe.closed.get(WAIT, TimeUnit.SECONDS));
    }

    @Test
    void endlessLineIsCutOff() throws Exception {
        Socket[] s = pair();
        Probe hostProbe = new Probe();
        connect(s[0], hostProbe, 2_000, 8_000);
        OutputStream raw = s[1].getOutputStream();
        try {
            raw.write("A".repeat(10_000).getBytes(StandardCharsets.UTF_8));
            raw.flush();
        } catch (java.io.IOException ignored) {
            // the host may already have hung up
        }
        assertEquals("protocol", hostProbe.closed.get(WAIT, TimeUnit.SECONDS));
    }
}
