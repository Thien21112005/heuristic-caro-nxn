package net;

import net.Protocol.Bye;
import net.Protocol.Kind;
import net.Protocol.Message;
import net.Protocol.ProtocolException;
import net.Protocol.Simple;

import javax.swing.SwingUtilities;
import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * One TCP link to the other player. Reads on its own thread, writes through a
 * single-threaded queue, answers PINGs and closes itself when the peer goes
 * quiet. Listener callbacks are delivered through the dispatcher (the EDT by default).
 */
public class Connection implements AutoCloseable {
    public interface Listener {
        void onMessage(Message message);
        /** The link is gone; {@code reason} is "bye", "timeout", "protocol", "io" or "local". */
        void onClosed(String reason);
    }

    public static final long HEARTBEAT_MS = 2_000;
    public static final long TIMEOUT_MS = 8_000;

    private final Socket socket;
    private final Consumer<Runnable> dispatcher;
    private final long heartbeatMs;
    private final long timeoutMs;
    private final Writer out;
    private final Reader in;
    private final ExecutorService sender = Executors.newSingleThreadExecutor(daemon("lan-send"));
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(daemon("lan-heartbeat"));
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile Listener listener;
    private volatile long lastReceived = System.currentTimeMillis();

    public Connection(Socket socket) throws IOException {
        this(socket, SwingUtilities::invokeLater, HEARTBEAT_MS, TIMEOUT_MS);
    }

    Connection(Socket socket, Consumer<Runnable> dispatcher, long heartbeatMs, long timeoutMs) throws IOException {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.heartbeatMs = heartbeatMs;
        this.timeoutMs = timeoutMs;
        socket.setTcpNoDelay(true);
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    /** Starts reading and heart-beating. Messages are only read once a listener is set. */
    public void start(Listener listener) {
        this.listener = listener;
        lastReceived = System.currentTimeMillis();
        Thread reader = new Thread(this::readLoop, "lan-read");
        reader.setDaemon(true);
        reader.start();
        heartbeat.scheduleAtFixedRate(this::beat, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
    }

    /** Hands the link to a new listener (e.g. from the lobby to the game). */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public SocketAddress getRemoteAddress() {
        return socket.getRemoteSocketAddress();
    }

    public boolean isOpen() {
        return !closed.get();
    }

    public void send(Message message) {
        if (closed.get()) return;
        String line = Protocol.encode(message);
        try {
            sender.execute(() -> {
                try {
                    out.write(line);
                    out.write('\n');
                    out.flush();
                } catch (IOException e) {
                    shutdown("io");
                }
            });
        } catch (RejectedExecutionException ignored) {
            // closed concurrently
        }
    }

    /** Says goodbye to the peer and closes. */
    @Override
    public void close() {
        close("");
    }

    public void close(String byeReason) {
        if (closed.get()) return;
        send(new Bye(byeReason));
        sender.shutdown();
        try {
            sender.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        shutdown("local");
    }

    private void readLoop() {
        StringBuilder line = new StringBuilder();
        try {
            int ch;
            while ((ch = in.read()) != -1) {
                if (ch == '\n') {
                    handle(line.toString().replace("\r", ""));
                    line.setLength(0);
                    if (closed.get()) return;
                } else if (line.length() > Protocol.MAX_LINE) {
                    // Never buffer unbounded input from the network
                    send(new Bye("protocol"));
                    shutdown("protocol");
                    return;
                } else {
                    line.append((char) ch);
                }
            }
            shutdown("io");
        } catch (IOException e) {
            shutdown("io");
        }
    }

    private void handle(String raw) {
        lastReceived = System.currentTimeMillis();
        if (raw.isEmpty()) return;
        Message message;
        try {
            message = Protocol.decode(raw);
        } catch (ProtocolException e) {
            send(new Bye("protocol"));
            shutdown("protocol");
            return;
        }
        if (message instanceof Simple s && s.kind() == Kind.PING) {
            send(Protocol.PONG);
            return;
        }
        if (message instanceof Simple s && s.kind() == Kind.PONG) return;
        deliver(message);
        if (message instanceof Bye) shutdown("bye");
    }

    private void beat() {
        if (System.currentTimeMillis() - lastReceived > timeoutMs) {
            shutdown("timeout");
        } else {
            send(Protocol.PING);
        }
    }

    private void deliver(Message message) {
        dispatcher.accept(() -> {
            Listener l = listener;
            if (l != null) l.onMessage(message);
        });
    }

    private void shutdown(String reason) {
        if (!closed.compareAndSet(false, true)) return;
        heartbeat.shutdownNow();
        sender.shutdownNow();
        try {
            socket.close();
        } catch (IOException ignored) {
            // already closed
        }
        dispatcher.accept(() -> {
            Listener l = listener;
            if (l != null) l.onClosed(reason);
        });
    }

    private static ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }
}
