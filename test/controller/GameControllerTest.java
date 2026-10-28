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
    void slowAiResultIsDiscardedAfterRestart() throws Exception {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        model.ai.AiStrategy slow = (board, me) -> {
            calls.incrementAndGet();
            try { Thread.sleep(500); } catch (InterruptedException ignored) { }
            return new int[]{0, 0};
        };
        GameController gc = onEdt(() -> new GameController(10, GameMode.VS_AI, slow));
        onEdt(() -> { gc.play(5, 5); return null; });
        Thread.sleep(100); // the engine is now busy
        onEdt(() -> { gc.restart(); return null; });
        Thread.sleep(AI_WAIT_MS);
        assertEquals(1, calls.get());
        assertEquals(0, (int) onEdt(() -> gc.getModel().getMoveCount()), "stale AI move must be dropped");
        assertFalse(onEdt(gc::isAiThinking));
        assertTrue(onEdt(gc::acceptsInput));
    }

    @Test
    void uiStaysResponsiveWhileAiSearches() throws Exception {
        model.ai.AiStrategy slow = (board, me) -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) { }
            return new int[]{0, 0};
        };
        GameController gc = onEdt(() -> new GameController(10, GameMode.VS_AI, slow));
        onEdt(() -> { gc.play(5, 5); return null; });
        long t0 = System.nanoTime();
        onEdt(() -> null); // would block ~600 ms if the search ran on the EDT
        assertTrue((System.nanoTime() - t0) / 1_000_000 < 200, "EDT was blocked by the AI");
        Thread.sleep(AI_WAIT_MS);
        assertEquals(GameModel.O, (int) onEdt(() -> gc.getModel().getCell(0, 0)));
    }

    private static GameController aiOpens() throws Exception {
        return onEdt(() -> {
            GameController gc = new GameController(10, GameMode.VS_AI,
                    model.ai.Difficulty.NORMAL.createAi(), GameModel.O);
            gc.start();
            return gc;
        });
    }

    @Test
    void aiPlayingXOpensTheGame() throws Exception {
        GameController gc = aiOpens();
        assertTrue(onEdt(gc::isAiThinking));
        assertFalse(onEdt(gc::acceptsInput));
        Thread.sleep(AI_WAIT_MS);
        assertEquals(1, (int) onEdt(() -> gc.getModel().movesBy(GameModel.X)));
        assertEquals(GameModel.O, (int) onEdt(() -> gc.getModel().getCurrentPlayer()));
        assertTrue(onEdt(gc::acceptsInput));
    }

    @Test
    void cannotUndoTheAiOpeningMove() throws Exception {
        GameController gc = aiOpens();
        Thread.sleep(AI_WAIT_MS);
        assertFalse(onEdt(gc::canUndo));
        onEdt(() -> { gc.undo(); return null; });
        assertEquals(1, (int) onEdt(() -> gc.getModel().getMoveCount()), "opening move must stay");
    }

    @Test
    void undoWhenAiMovedFirstReturnsToThePlayersTurn() throws Exception {
        GameController gc = aiOpens();
        Thread.sleep(AI_WAIT_MS);
        int[] free = onEdt(() -> {
            int[][] b = gc.getModel().getBoard();
            for (int r = 0; r < 10; r++) for (int c = 0; c < 10; c++) if (b[r][c] == 0) return new int[]{r, c};
            return null;
        });
        onEdt(() -> { gc.play(free[0], free[1]); return null; });
        Thread.sleep(AI_WAIT_MS); // AI replies
        assertEquals(3, (int) onEdt(() -> gc.getModel().getMoveCount()));
        onEdt(() -> { gc.undo(); return null; });
        assertEquals(1, (int) onEdt(() -> gc.getModel().getMoveCount()), "undo removes the reply and the player's move");
        assertEquals(GameModel.O, (int) onEdt(() -> gc.getModel().getCurrentPlayer()));
        assertFalse(onEdt(gc::isAiThinking));
    }

    @Test
    void restartLetsTheAiOpenAgain() throws Exception {
        GameController gc = aiOpens();
        Thread.sleep(AI_WAIT_MS);
        onEdt(() -> { gc.restart(); return null; });
        assertTrue(onEdt(gc::isAiThinking));
        Thread.sleep(AI_WAIT_MS);
        assertEquals(1, (int) onEdt(() -> gc.getModel().getMoveCount()));
    }

    @Test
    void newRoundWithSwapHandsXToTheAi() throws Exception {
        GameController gc = onEdt(() -> new GameController(10, GameMode.VS_AI));
        onEdt(() -> { gc.play(5, 5); return null; });
        Thread.sleep(AI_WAIT_MS);
        onEdt(() -> { gc.newRound(true); return null; });
        assertEquals(GameModel.O, (int) onEdt(gc::getHumanSide));
        assertEquals(GameModel.X, (int) onEdt(gc::getAiSide));
        assertTrue(onEdt(gc::isAiThinking), "AI now plays X and must open");
        Thread.sleep(AI_WAIT_MS);
        assertEquals(1, (int) onEdt(() -> gc.getModel().movesBy(GameModel.X)));
        onEdt(() -> { gc.newRound(true); return null; });
        assertEquals(GameModel.X, (int) onEdt(gc::getHumanSide), "sides swap back the next round");
        assertTrue(onEdt(gc::acceptsInput));
    }

    @Test
    void newRoundWithoutSwapKeepsSides() throws Exception {
        GameController gc = onEdt(() -> new GameController(3, GameMode.TWO_PLAYERS));
        onEdt(() -> { gc.play(0, 0); gc.newRound(false); return null; });
        assertEquals(GameModel.X, (int) onEdt(gc::getHumanSide));
        assertEquals(0, (int) onEdt(() -> gc.getModel().getMoveCount()));
    }

    @Test
    void resignAgainstTheAiAlwaysLosesForTheHuman() throws Exception {
        GameController gc = onEdt(() -> new GameController(10, GameMode.VS_AI));
        onEdt(() -> { gc.play(5, 5); gc.resign(); return null; }); // AI is thinking right now
        assertEquals(GameModel.O, (int) onEdt(() -> gc.getModel().getWinner()));
        assertEquals(model.EndReason.RESIGN, onEdt(() -> gc.getModel().getEndReason()));
        Thread.sleep(AI_WAIT_MS);
        assertEquals(1, (int) onEdt(() -> gc.getModel().getMoveCount()), "the AI must not move after a resignation");
    }

    @Test
    void twoPlayerResignLosesForThePlayerToMove() throws Exception {
        GameController gc = onEdt(() -> new GameController(10, GameMode.TWO_PLAYERS));
        onEdt(() -> { gc.play(0, 0); gc.resign(); return null; }); // O to move resigns
        assertEquals(GameModel.X, (int) onEdt(() -> gc.getModel().getWinner()));
    }

    @Test
    void twoPlayerUndoTakesBackOneMove() throws Exception {
        GameController gc = onEdt(() -> new GameController(3, GameMode.TWO_PLAYERS));
        onEdt(() -> { gc.play(0, 0); gc.play(1, 1); gc.undo(); return null; });
        assertEquals(1, (int) onEdt(() -> gc.getModel().getMoveCount()));
        assertEquals(GameModel.O, (int) onEdt(() -> gc.getModel().getCurrentPlayer()));
    }
}
