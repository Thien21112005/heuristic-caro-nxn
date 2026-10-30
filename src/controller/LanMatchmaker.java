package controller;

import net.Connection;
import net.LanClient;
import net.LanDiscovery;
import net.LanHost;
import net.Protocol;
import net.Protocol.Hello;
import net.Protocol.Message;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Sets up a LAN game: hosting (listen + announce) or joining, then the HELLO
 * exchange. On success hands over a ready NetworkGameController. Callbacks
 * run on the EDT. Error codes: "busy", "unreachable", "version", "handshake", "closed".
 */
public class LanMatchmaker implements AutoCloseable {
    public interface Callbacks {
        void onReady(NetworkGameController game);
        void onError(String code);
    }

    static final int HANDSHAKE_TIMEOUT_MS = 5_000;

    private final Callbacks callbacks;
    private LanHost host;
    private LanDiscovery.Announcer announcer;
    private Connection connection;
    private Timer handshakeTimer;
    private volatile boolean finished;
    private boolean handedOver; // the connection now belongs to the game

    public LanMatchmaker(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    /** Opens a room on every interface and announces it. Returns the TCP port. */
    public int host(String name, int size, int turnSeconds) throws IOException {
        return host(name, size, turnSeconds, null, true);
    }

    /** For tests: bind to {@code bindAddress} and optionally skip the UDP announcement. */
    int host(String name, int size, int turnSeconds, InetAddress bindAddress, boolean announce) throws IOException {
        host = new LanHost(bindAddress, bindAddress == null ? LanHost.DEFAULT_PORT : 0);
        if (announce) announcer = new LanDiscovery.Announcer(name, size, host.getPort());
        host.acceptAsync(socket -> SwingUtilities.invokeLater(() -> {
            stopAdvertising();
            Hello mine = new Hello(Protocol.VERSION, name, size, turnSeconds);
            open(socket, mine, theirs -> new NetworkGameController(connection, true, size, turnSeconds, theirs.name()));
        }), e -> fail("busy"));
        return host.getPort();
    }

    /** Connects to a host in the background. */
    public void join(String name, String address, int port) {
        Thread t = new Thread(() -> {
            try {
                Socket socket = LanClient.connect(address, port);
                SwingUtilities.invokeLater(() -> open(socket, null, theirs -> {
                    if (theirs.size() < 3) return null; // a host always sends its board size
                    connection.send(new Hello(Protocol.VERSION, name, 0, 0));
                    return new NetworkGameController(connection, false, theirs.size(), theirs.turnSeconds(), theirs.name());
                }));
            } catch (IOException e) {
                fail("unreachable");
            }
        }, "lan-join");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Wraps the socket and waits for the other side's HELLO.
     * @param mine sent immediately (host), or null to answer after theirs arrives (client)
     */
    private void open(Socket socket, Hello mine, java.util.function.Function<Hello, NetworkGameController> build) {
        if (finished) {
            closeQuietly(socket);
            return;
        }
        try {
            connection = new Connection(socket);
        } catch (IOException e) {
            fail("unreachable");
            return;
        }
        handshakeTimer = new Timer(HANDSHAKE_TIMEOUT_MS, e -> fail("handshake"));
        handshakeTimer.setRepeats(false);
        handshakeTimer.start();
        connection.start(new Connection.Listener() {
            @Override
            public void onMessage(Message message) {
                if (finished) return;
                if (!(message instanceof Hello theirs)) {
                    fail("handshake");
                    return;
                }
                if (theirs.version() != Protocol.VERSION) {
                    fail("version");
                    return;
                }
                NetworkGameController game = build.apply(theirs);
                if (game == null) {
                    fail("handshake");
                    return;
                }
                finished = true;
                handedOver = true;
                handshakeTimer.stop();
                callbacks.onReady(game);
            }

            @Override
            public void onClosed(String reason) {
                fail("closed");
            }
        });
        if (mine != null) connection.send(mine);
    }

    private void fail(String code) {
        if (finished) return;
        finished = true;
        Runnable report = () -> {
            close();
            callbacks.onError(code);
        };
        if (SwingUtilities.isEventDispatchThread()) report.run();
        else SwingUtilities.invokeLater(report);
    }

    private void stopAdvertising() {
        if (announcer != null) announcer.close();
        announcer = null;
        if (host != null) host.close();
        host = null;
    }

    /** Cancels hosting or joining. Does not close a connection already handed to a game. */
    @Override
    public void close() {
        finished = true;
        if (handshakeTimer != null) handshakeTimer.stop();
        stopAdvertising();
        if (connection != null && !handedOver) connection.close("cancelled");
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // nothing to do
        }
    }
}
