package controller;

import model.GameModel;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LanMatchmakerTest {

    private static LanMatchmaker.Callbacks into(CompletableFuture<NetworkGameController> ready,
                                                CompletableFuture<String> error) {
        return new LanMatchmaker.Callbacks() {
            @Override
            public void onReady(NetworkGameController game) { ready.complete(game); }

            @Override
            public void onError(String code) { error.complete(code); }
        };
    }

    @Test
    void hostAndGuestAgreeOnTheRulesAfterHello() throws Exception {
        CompletableFuture<NetworkGameController> hostReady = new CompletableFuture<>(), guestReady = new CompletableFuture<>();
        CompletableFuture<String> hostError = new CompletableFuture<>(), guestError = new CompletableFuture<>();
        LanMatchmaker hosting = new LanMatchmaker(into(hostReady, hostError));
        LanMatchmaker joining = new LanMatchmaker(into(guestReady, guestError));

        int port = hosting.host("Alice", 15, 30, InetAddress.getLoopbackAddress(), false);
        joining.join("Bob", "127.0.0.1", port);

        NetworkGameController host = hostReady.get(5, TimeUnit.SECONDS);
        NetworkGameController guest = guestReady.get(5, TimeUnit.SECONDS);
        assertEquals("Bob", host.getOpponentName());
        assertEquals("Alice", guest.getOpponentName());
        assertEquals(15, guest.getModel().getSize(), "the guest takes the host's board size");
        assertEquals(30, guest.getTurnSeconds());
        assertTrue(host.isHost());
        assertFalse(guest.isHost());
        assertEquals(GameModel.X, host.getHumanSide());
        assertFalse(hostError.isDone());
        assertFalse(guestError.isDone());

        SwingUtilities.invokeAndWait(() -> {
            host.dispose();
            guest.dispose();
            hosting.close();
            joining.close();
        });
    }

    @Test
    void joiningANonexistentHostReportsUnreachable() throws Exception {
        int unused;
        try (ServerSocket probe = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            unused = probe.getLocalPort(); // closed again right away, so nothing listens here
        }
        CompletableFuture<NetworkGameController> ready = new CompletableFuture<>();
        CompletableFuture<String> error = new CompletableFuture<>();
        new LanMatchmaker(into(ready, error)).join("Bob", "127.0.0.1", unused);
        assertEquals("unreachable", error.get(6, TimeUnit.SECONDS));
        assertFalse(ready.isDone());
    }
}
