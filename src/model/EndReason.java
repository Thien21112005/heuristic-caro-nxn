package model;

/** Why a game ended. NONE while it is still running. */
public enum EndReason {
    NONE,
    /** A player completed a line. */
    LINE,
    /** The board filled up. */
    DRAW,
    /** The player to move ran out of time. */
    TIMEOUT,
    /** A player gave up. */
    RESIGN,
    /** The network opponent left (LAN games). */
    DISCONNECT
}
