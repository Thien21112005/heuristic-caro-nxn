package net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Opens the TCP link to a host. Blocking; call it off the EDT. */
public final class LanClient {
    public static final int CONNECT_TIMEOUT_MS = 4_000;

    private LanClient() {}

    public static Socket connect(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }
}
