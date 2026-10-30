package net;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LanDiscoveryTest {

    @Test
    void announcementsRoundTrip() {
        String text = LanDiscovery.announcement("Phòng của Thiện", 15, 45678);
        LanDiscovery.Room room = LanDiscovery.parse(text, "192.168.1.5", 42);
        assertNotNull(room);
        assertEquals("Phòng của Thiện", room.name());
        assertEquals(15, room.size());
        assertEquals("192.168.1.5", room.address());
        assertEquals(45678, room.port());
    }

    @Test
    void ignoresForeignOrBrokenPackets() {
        assertNull(LanDiscovery.parse("HELLO WORLD", "1.2.3.4", 0));
        assertNull(LanDiscovery.parse("CARO_HOST 999 Bob 15 45678", "1.2.3.4", 0), "other protocol version");
        assertNull(LanDiscovery.parse("CARO_HOST 1 Bob 99 45678", "1.2.3.4", 0));
        assertNull(LanDiscovery.parse("CARO_HOST 1 Bob 15 70000", "1.2.3.4", 0));
        assertNull(LanDiscovery.parse("CARO_HOST 1 Bob x 45678", "1.2.3.4", 0));
    }

    @Test
    void browserSeesAnAnnouncerAndForgetsItWhenItStops() throws Exception {
        LinkedBlockingQueue<List<LanDiscovery.Room>> updates = new LinkedBlockingQueue<>();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (LanDiscovery.Browser browser = new LanDiscovery.Browser(
                new InetSocketAddress(loopback, 0), updates::add, Runnable::run)) {
            LanDiscovery.Announcer announcer = new LanDiscovery.Announcer("Host", 10, 50000,
                    List.of(new InetSocketAddress(loopback, browser.getPort())));
            List<LanDiscovery.Room> rooms = updates.poll(3, TimeUnit.SECONDS);
            assertNotNull(rooms, "the browser never heard the host");
            assertEquals(1, rooms.size());
            assertEquals("Host", rooms.get(0).name());
            assertEquals(50000, rooms.get(0).port());

            announcer.close();
            List<LanDiscovery.Room> afterStop = updates.poll(LanDiscovery.EXPIRE_MS + 2_000, TimeUnit.MILLISECONDS);
            assertNotNull(afterStop, "the room never expired");
            assertTrue(afterStop.isEmpty());
        }
    }
}
