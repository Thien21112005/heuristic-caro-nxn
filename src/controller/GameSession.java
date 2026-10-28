package controller;

import model.GameMode;
import model.GameModel;

import java.util.function.Consumer;

/**
 * What the game screen needs from a running game, whether it is local
 * (against the AI or a second player) or played over the network.
 */
public interface GameSession {

    interface Listener {
        /** Board, turn or result changed — redraw. */
        void onStateChanged();
        /** The game just ended (win, draw or forfeit). */
        void onGameOver();
        void onAiThinking(boolean thinking);
        /** Called about ten times a second while a timed turn is running. */
        default void onClockTick(long remainingMs) {}
    }

    void setListener(Listener listener);
    GameModel getModel();
    GameMode getMode();

    /** Piece of seat 0 (you / Player 1) this round. */
    int getHumanSide();

    /** Call once the listener is attached. */
    void start();

    /** True when a click on the board should be accepted right now. */
    boolean acceptsInput();
    void play(int r, int c);

    boolean canUndo();
    void undo();
    void restart();
    /** Next round, optionally swapping pieces. */
    void newRound(boolean swapSides);
    void resign();

    boolean isAiThinking();
    /** Best move for the side to play; the callback runs on the EDT. */
    void requestHint(Consumer<int[]> onHint);

    int getTurnSeconds();
    /** Milliseconds left in the current timed turn, or -1 when no clock is running. */
    long getRemainingMillis();
    void pauseClock();
    void resumeClock();

    /** Stops timers and connections; call when leaving the screen. */
    void dispose();
}
