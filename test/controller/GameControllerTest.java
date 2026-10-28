package controller;

import model.GameMode;
import model.GameModel;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {
    /** Longer than the AI's maximum thinking delay. */
    private static final long AI_WAIT_MS = 1200;

    private static <T> T onEdt(java.util.concurrent.Callable<T> task) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(task.call());
            } catch (Exception e) {
                error.set(e);
            }
        });
        if (error.get() != null) throw error.get();
        return result.get();
    }

    @Test
    void aiRepliesAfterHumanMove() throws Exception {
        GameController gc = onEdt(() -> new GameController(10, GameMode.VS_AI));
        onEdt(() -> { gc.play(5, 5); return null; });
        assertTrue(onEdt(gc::isAiThinking));
        assertFalse(onEdt(gc::acceptsInput), "human must wait for the AI");
        Thread.sleep(AI_WAIT_MS);
        assertEquals(2, (int) onEdt(() -> gc.getModel().getMoveCount()));
        assertEquals(GameModel.X, (int) onEdt(() -> gc.getModel().getCurrentPlayer()));
        assertTrue(onEdt(gc::acceptsInput));
    }

    @Test
    void restartWhileAiThinkingCancelsItsMove() throws Exception {
        GameController gc = onEdt(() -> new GameController(10, GameMode.VS_AI));
        onEdt(() -> { gc.play(5, 5); gc.restart(); return null; });
        Thread.sleep(AI_WAIT_MS);
        assertEquals(0, (int) onEdt(() -> gc.getModel().getMoveCount()));
        assertTrue(onEdt(gc::acceptsInput), "human must not be locked out after restart");
    }

    @Test
    void undoWhileAiThinkingTakesBackOnlyTheHumanMove() throws Exception {
        GameController gc = onEdt(() -> new GameController(10, GameMode.VS_AI));
        onEdt(() -> { gc.play(5, 5); gc.undo(); return null; });
        Thread.sleep(AI_WAIT_MS);
        assertEquals(0, (int) onEdt(() -> gc.getModel().getMoveCount()));
        assertTrue(onEdt(gc::acceptsInput));
    }

    @Test
    void undoAfterAiReplyTakesBackBothMoves() throws Exception {
        GameController gc = onEdt(() -> new GameController(10, GameMode.VS_AI));
        onEdt(() -> { gc.play(5, 5); return null; });
        Thread.sleep(AI_WAIT_MS);
        onEdt(() -> { gc.undo(); return null; });
        assertEquals(0, (int) onEdt(() -> gc.getModel().getMoveCount()));
        assertEquals(GameModel.X, (int) onEdt(() -> gc.getModel().getCurrentPlayer()));
    }

    @Test
    void twoPlayerUndoTakesBackOneMove() throws Exception {
        GameController gc = onEdt(() -> new GameController(3, GameMode.TWO_PLAYERS));
        onEdt(() -> { gc.play(0, 0); gc.play(1, 1); gc.undo(); return null; });
        assertEquals(1, (int) onEdt(() -> gc.getModel().getMoveCount()));
        assertEquals(GameModel.O, (int) onEdt(() -> gc.getModel().getCurrentPlayer()));
    }
}
