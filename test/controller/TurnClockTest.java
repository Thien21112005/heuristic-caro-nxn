package controller;

import model.EndReason;
import model.GameConfig;
import model.GameModel;
import model.ai.Difficulty;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TurnClockTest {
    private final AtomicLong time = new AtomicLong(1_000_000);

    private GameController create(GameConfig config) throws Exception {
        AtomicReference<GameController> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            GameController gc = new GameController(config);
            gc.setTimeSource(time::get);
            gc.start();
            ref.set(gc);
        });
        return ref.get();
    }

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void advance(GameController gc, long ms) throws Exception {
        time.addAndGet(ms);
        edt(gc::tickClock);
    }

    @Test
    void playerToMoveLosesWhenTimeRunsOut() throws Exception {
        GameController gc = create(GameConfig.twoPlayers(10).withTurnSeconds(10));
        assertEquals(10_000, gc.getRemainingMillis());
        advance(gc, 9_900);
        assertFalse(gc.getModel().isOver());
        assertEquals(100, gc.getRemainingMillis());
        advance(gc, 200);
        assertTrue(gc.getModel().isOver());
        assertEquals(GameModel.O, gc.getModel().getWinner(), "X ran out of time");
        assertEquals(EndReason.TIMEOUT, gc.getModel().getEndReason());
        assertEquals(-1, gc.getRemainingMillis(), "no clock after the game ends");
        edt(gc::dispose);
    }

    @Test
    void everyMoveGivesTheNextPlayerAFullTurn() throws Exception {
        GameController gc = create(GameConfig.twoPlayers(10).withTurnSeconds(20));
        advance(gc, 15_000);
        edt(() -> gc.play(5, 5));
        assertEquals(20_000, gc.getRemainingMillis());
        advance(gc, 19_000);
        assertFalse(gc.getModel().isOver());
        edt(gc::dispose);
    }

    @Test
    void pauseFreezesTheClock() throws Exception {
        GameController gc = create(GameConfig.twoPlayers(10).withTurnSeconds(10));
        advance(gc, 4_000);
        edt(gc::pauseClock);
        advance(gc, 60_000);
        assertFalse(gc.getModel().isOver());
        assertEquals(6_000, gc.getRemainingMillis());
        edt(gc::resumeClock);
        advance(gc, 5_000);
        assertEquals(1_000, gc.getRemainingMillis());
        edt(gc::dispose);
    }

    @Test
    void undoAfterTimeoutResumesWithAFreshClock() throws Exception {
        GameController gc = create(GameConfig.twoPlayers(10).withTurnSeconds(10));
        edt(() -> gc.play(1, 1));
        advance(gc, 11_000);
        assertEquals(EndReason.TIMEOUT, gc.getModel().getEndReason());
        edt(gc::undo);
        assertFalse(gc.getModel().isOver());
        assertEquals(1, gc.getModel().getMoveCount(), "undo after a timeout keeps the board");
        assertEquals(10_000, gc.getRemainingMillis());
        edt(gc::dispose);
    }

    @Test
    void restartResetsTheClock() throws Exception {
        GameController gc = create(GameConfig.twoPlayers(10).withTurnSeconds(30));
        advance(gc, 25_000);
        edt(gc::restart);
        assertEquals(30_000, gc.getRemainingMillis());
        edt(gc::dispose);
    }

    @Test
    void theAiIsNeverTimed() throws Exception {
        GameController gc = create(GameConfig.vsAi(10, Difficulty.NORMAL, GameModel.O, false).withTurnSeconds(10));
        assertEquals(-1, gc.getRemainingMillis(), "AI opens as X, so no clock yet");
        Thread.sleep(1200); // let the AI play
        assertEquals(10_000, gc.getRemainingMillis(), "the human's turn is timed");
        edt(gc::dispose);
    }

    @Test
    void noClockWhenDisabled() throws Exception {
        GameController gc = create(GameConfig.twoPlayers(10));
        assertEquals(-1, gc.getRemainingMillis());
        advance(gc, 3_600_000);
        assertFalse(gc.getModel().isOver());
        edt(gc::dispose);
    }
}
