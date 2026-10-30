package net;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Finds games on the local network: a host broadcasts
 * {@code CARO_HOST <version> <name> <size> <port>} over UDP every second and
 * browsers list every room heard from in the last few seconds.
 */
public final class LanDiscovery {
    public static final int PORT = 45679;
    static final long ANNOUNCE_MS = 1_000;
    static final long EXPIRE_MS = 3_500;
    private static final String PREFIX = "CARO_HOST";

    private LanDiscovery() {}

    /** A host heard on the network. */
    public record Room(String name, int size, String address, int port, long lastSeen) {
        public String key() { return address + ":" + port; }
    }

    static String announcement(String name, int size, int port) {
        return PREFIX + " " + Protocol.VERSION + " "
                + URLEncoder.encode(Protocol.cleanName(name), StandardCharsets.UTF_8) + " " + size + " " + port;
    }

    /** Parses an announcement, or returns null for anything else (other apps, other versions). */
    static Room parse(String text, String address, long now) {
        String[] t = text.trim().split(" ");
        if (t.length != 5 || !PREFIX.equals(t[0])) return null;
        try {
            if (Integer.parseInt(t[1]) != Protocol.VERSION) return null;
            int size = Integer.parseInt(t[3]);
            int port = Integer.parseInt(t[4]);
            if (size < 3 || size > 20 || port < 1 || port > 65535) return null;
            return new Room(Protocol.cleanName(URLDecoder.decode(t[2], StandardCharsets.UTF_8)), size, address, port, now);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Broadcasts a room until closed. */
    public static final class Announcer implements AutoCloseable {
        private final DatagramSocket socket;
        private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lan-announce");
            t.setDaemon(true);
            return t;
        });

        public Announcer(String name, int size, int tcpPort) throws IOException {
            this(name, size, tcpPort, broadcastTargets(PORT));
        }

        /** For tests: announce to explicit addresses instead of the broadcast addresses. */
        Announcer(String name, int size, int tcpPort, List<InetSocketAddress> targets) throws IOException {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] data = announcement(name, size, tcpPort).getBytes(StandardCharsets.UTF_8);
            timer.scheduleAtFixedRate(() -> {
                for (InetSocketAddress target : targets) {
                    try {
                        socket.send(new DatagramPacket(data, data.length, target));
                    } catch (IOException ignored) {
                        // an interface may be down; keep trying the others
                    }
                }
            }, 0, ANNOUNCE_MS, TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            timer.shutdownNow();
            socket.close();
        }

        private static List<InetSocketAddress> broadcastTargets(int port) {
            Set<InetSocketAddress> out = new LinkedHashSet<>();
            out.add(new InetSocketAddress("255.255.255.255", port));
            try {
                for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    if (!ni.isUp() || ni.isLoopback()) continue;
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        // Directed broadcasts get through routers that drop 255.255.255.255
                        if (ia.getBroadcast() != null) out.add(new InetSocketAddress(ia.getBroadcast(), port));
                    }
                }
            } catch (SocketException ignored) {
                // fall back to the limited broadcast address only
            }
            return new ArrayList<>(out);
        }
    }

    /** Listens for announcements and reports the current room list whenever it changes. */
    public static final class Browser implements AutoCloseable {
        private final DatagramSocket socket;
        private final Consumer<List<Room>> onChange;
        private final Consumer<Runnable> dispatcher;
        private final Map<String, Room> rooms = new LinkedHashMap<>();
        private final ScheduledExecutorService expiry = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lan-browse-expiry");
            t.setDaemon(true);
            return t;
        });

        public Browser(Consumer<List<Room>> onChange) throws IOException {
            this(new InetSocketAddress(PORT), onChange, SwingUtilities::invokeLater);
        }

        Browser(InetSocketAddress bind, Consumer<List<Room>> onChange, Consumer<Runnable> dispatcher) throws IOException {
            this.onChange = onChange;
            this.dispatcher = dispatcher;
            socket = new DatagramSocket(null);
            // Several copies of the game on one machine may browse at the same time
            socket.setReuseAddress(true);
            socket.bind(bind);
            Thread reader = new Thread(this::receiveLoop, "lan-browse");
            reader.setDaemon(true);
            reader.start();
            expiry.scheduleAtFixedRate(this::expire, 500, 500, TimeUnit.MILLISECONDS);
        }

        int getPort() {
            return socket.getLocalPort();
        }

        private void receiveLoop() {
            byte[] buffer = new byte[512];
            while (!socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    return;
                }
                String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                Room room = parse(text, packet.getAddress().getHostAddress(), System.currentTimeMillis());
                if (room == null) continue;
                boolean changed;
                synchronized (rooms) {
                    Room before = rooms.put(room.key(), room);
                    changed = before == null || !before.name().equals(room.name()) || before.size() != room.size();
                }
                if (changed) publish();
            }
        }

        private void expire() {
            long now = System.currentTimeMillis();
            boolean changed;
            synchronized (rooms) {
                changed = rooms.values().removeIf(r -> now - r.lastSeen() > EXPIRE_MS);
            }
            if (changed) publish();
        }

        private void publish() {
            List<Room> snapshot;
            synchronized (rooms) {
                snapshot = new ArrayList<>(rooms.values());
            }
            dispatcher.accept(() -> onChange.accept(snapshot));
        }

        @Override
        public void close() {
            expiry.shutdownNow();
            socket.close();
        }
    }
}
