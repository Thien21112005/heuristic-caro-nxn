package net;

import net.Protocol.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {

    private static Message roundTrip(Message m) throws ProtocolException {
        return Protocol.decode(Protocol.encode(m));
    }

    @Test
    void everyMessageSurvivesARoundTrip() throws ProtocolException {
        assertEquals(new Hello(1, "Thiện", 15, 30), roundTrip(new Hello(1, "Thiện", 15, 30)));
        assertEquals(new Move(0, 19), roundTrip(new Move(0, 19)));
        for (Kind k : Kind.values()) assertEquals(new Simple(k), roundTrip(new Simple(k)));
        assertEquals(new Bye("left the game"), roundTrip(new Bye("left the game")));
        assertEquals(new Bye(""), roundTrip(new Bye("")));
    }

    @Test
    void namesWithSpacesAndSymbolsAreEncoded() throws ProtocolException {
        String line = Protocol.encode(new Hello(1, "Nhà Chợt Lét #1", 10, 0));
        assertEquals(5, line.split(" ").length, "the name must not add tokens: " + line);
        assertEquals("Nhà Chợt Lét #1", ((Hello) Protocol.decode(line)).name());
    }

    @Test
    void namesAreCleanedAndShortened() {
        assertEquals("Player", Protocol.cleanName("   "));
        assertEquals("Player", Protocol.cleanName(null));
        assertEquals("abc", Protocol.cleanName("a\u0000b\nc"));
        assertEquals(Protocol.MAX_NAME, Protocol.cleanName("x".repeat(100)).length());
    }

    @Test
    void clientHelloMayLeaveSizeAndTimeAtZero() throws ProtocolException {
        Hello h = (Hello) Protocol.decode("HELLO 1 Bob 0 0");
        assertEquals(0, h.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "NOPE", "MOVE", "MOVE 1", "MOVE 1 2 3", "MOVE a b", "MOVE -1 0", "MOVE 0 20",
            "HELLO 1 Bob 15", "HELLO 1 Bob 2 0", "HELLO 1 Bob 21 0", "HELLO 1 Bob 15 9999",
            "PING extra", "BYE a b", "move 1 2"
    })
    void rejectsMalformedInput(String line) {
        assertThrows(ProtocolException.class, () -> Protocol.decode(line));
    }

    @Test
    void rejectsOverlongLines() {
        assertThrows(ProtocolException.class, () -> Protocol.decode("BYE " + "x".repeat(Protocol.MAX_LINE)));
        assertThrows(ProtocolException.class, () -> Protocol.decode(null));
    }
}
