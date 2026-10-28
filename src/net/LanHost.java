package net;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Listens for exactly one opponent. Tries the default port first and falls
 * back to any free port, so two hosts on one machine don't clash.
 */
public class LanHost implements AutoCloseable {
    public static final int DEFAULT_PORT = 45678;

    private final ServerSocket server;
    private volatile boolean closed;

    public LanHost() throws IOException {
        this(null, DEFAULT_PORT);
    }

    /** @param bind address to listen on, or null for every interface */
    public LanHost(InetAddress bind, int port) throws IOException {
        server = open(bind, port);
    }

    private static ServerSocket open(InetAddress bind, int port) throws IOException {
        try {
            return new ServerSocket(port, 1, bind);
        } catch (BindException busy) {
            if (port == 0) throw busy;
            return new ServerSocket(0, 1, bind);
        }
    }

    public int getPort() {
        return server.getLocalPort();
    }

    /** Waits for one opponent on a background thread; stops listening once someone connects. */
    public void acceptAsync(Consumer<Socket> onAccepted, Consumer<IOException> onError) {
        Thread t = new Thread(() -> {
            try {
                Socket socket = server.accept();
                close();
                onAccepted.accept(socket);
            } catch (IOException e) {
                if (!closed) onError.accept(e);
            }
        }, "lan-accept");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void close() {
        closed = true;
        try {
            server.close();
        } catch (IOException ignored) {
            // already closed
        }
    }

    /** IPv4 addresses other machines on the network can use to reach this one. */
    public static List<String> localAddresses() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) {
                        out.add(a.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
            // no interfaces available
        }
        return out;
    }
}
