package net;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Line-based LAN protocol, one message per line, space separated:
 * <pre>
 * HELLO &lt;version&gt; &lt;name&gt; &lt;size&gt; &lt;turnSeconds&gt;   both sides, host first; client sends size/time 0
 * MOVE &lt;row&gt; &lt;col&gt;
 * RESTART_REQ | RESTART_OK | RESTART_NO
 * RESIGN | TIMEOUT
 * PING | PONG
 * BYE [reason]
 * </pre>
 * Names are URL-encoded so they never contain spaces. Pieces are not sent:
 * the host plays X in odd rounds and O in even rounds on both machines.
 */
public final class Protocol {
    public static final int VERSION = 1;
    public static final int MAX_LINE = 256;
    public static final int MAX_NAME = 24;

    private Protocol() {}

    public sealed interface Message permits Hello, Move, Simple, Bye {}

    public record Hello(int version, String name, int size, int turnSeconds) implements Message {}

    public record Move(int row, int col) implements Message {}

    /** Messages without arguments. */
    public enum Kind { RESTART_REQ, RESTART_OK, RESTART_NO, RESIGN, TIMEOUT, PING, PONG }

    public record Simple(Kind kind) implements Message {}

    public record Bye(String reason) implements Message {}

    public static final Simple PING = new Simple(Kind.PING);
    public static final Simple PONG = new Simple(Kind.PONG);

    public static class ProtocolException extends Exception {
        public ProtocolException(String message) {
            super(message);
        }
    }

    /** Trims, strips control characters and shortens a player name for display and sending. */
    public static String cleanName(String name) {
        String n = name == null ? "" : name.replaceAll("\\p{Cntrl}", "").trim();
        if (n.length() > MAX_NAME) n = n.substring(0, MAX_NAME).trim();
        return n.isEmpty() ? "Player" : n;
    }

    public static String encode(Message m) {
        if (m instanceof Hello h) {
            return "HELLO " + h.version() + " " + URLEncoder.encode(cleanName(h.name()), StandardCharsets.UTF_8)
                    + " " + h.size() + " " + h.turnSeconds();
        }
        if (m instanceof Move mv) return "MOVE " + mv.row() + " " + mv.col();
        if (m instanceof Simple s) return s.kind().name();
        Bye b = (Bye) m;
        return b.reason() == null || b.reason().isEmpty() ? "BYE"
                : "BYE " + URLEncoder.encode(b.reason(), StandardCharsets.UTF_8);
    }

    public static Message decode(String line) throws ProtocolException {
        if (line == null) throw new ProtocolException("empty line");
        if (line.length() > MAX_LINE) throw new ProtocolException("line too long");
        String[] t = line.trim().split(" ");
        switch (t[0]) {
            case "HELLO": {
                expect(t, 5);
                String name = cleanName(URLDecoder.decode(t[2], StandardCharsets.UTF_8));
                int size = number(t[3], 0, 20);
                if (size != 0 && size < 3) throw new ProtocolException("bad board size " + size);
                return new Hello(number(t[1], 0, 999), name, size, number(t[4], 0, 600));
            }
            case "MOVE":
                expect(t, 3);
                return new Move(number(t[1], 0, 19), number(t[2], 0, 19));
            case "BYE":
                if (t.length > 2) throw new ProtocolException("BYE takes at most one argument");
                return new Bye(t.length == 2 ? URLDecoder.decode(t[1], StandardCharsets.UTF_8) : "");
            default:
                expect(t, 1);
                try {
                    return new Simple(Kind.valueOf(t[0]));
                } catch (IllegalArgumentException e) {
                    throw new ProtocolException("unknown message " + t[0]);
                }
        }
    }

    private static void expect(String[] tokens, int count) throws ProtocolException {
        if (tokens.length != count) {
            throw new ProtocolException(tokens[0] + " expects " + (count - 1) + " arguments");
        }
    }

    private static int number(String token, int min, int max) throws ProtocolException {
        try {
            int v = Integer.parseInt(token);
            if (v < min || v > max) throw new ProtocolException("value out of range: " + v);
            return v;
        } catch (NumberFormatException e) {
            throw new ProtocolException("not a number: " + token);
        }
    }
}
