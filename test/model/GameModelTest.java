package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameModelTest {

    /** Plays moves alternately starting with X; each pair is {row, col}. */
    private static GameModel play(int n, int[]... moves) {
        GameModel model = new GameModel(n);
        for (int[] m : moves) assertTrue(model.play(m[0], m[1]), "move rejected: " + m[0] + "," + m[1]);
        return model;
    }

    @Test
    void smallBoardNeedsFullLine() {
        GameModel model = play(3, new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}, new int[]{0, 2});
        assertEquals(GameModel.X, model.getWinner());
        assertArrayEquals(new int[]{0, 0, 0, 2}, model.getWinningLine());
    }

    @Test
    void largeBoardNeedsFiveInARowOnDiagonal() {
        GameModel model = new GameModel(10);
        for (int i = 0; i < 4; i++) {
            model.play(i, i);       // X
            model.play(i, 9);       // O
        }
        assertFalse(model.isOver());
        model.play(4, 4);
        assertEquals(GameModel.X, model.getWinner());
        assertArrayEquals(new int[]{0, 0, 4, 4}, model.getWinningLine());
    }

    @Test
    void fourInARowDoesNotWinOnLargeBoard() {
        GameModel model = new GameModel(10);
        for (int i = 0; i < 4; i++) {
            model.play(0, i);
            model.play(5, i);
        }
        assertFalse(model.isOver());
    }

    @Test
    void detectsDraw() {
        // X O X / X O O / O X X
        GameModel model = play(3,
                new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 2}, new int[]{1, 1}, new int[]{1, 0},
                new int[]{1, 2}, new int[]{2, 1}, new int[]{2, 0}, new int[]{2, 2});
        assertTrue(model.isDraw());
        assertEquals(GameModel.EMPTY, model.getWinner());
    }

    @Test
    void rejectsMovesOnOccupiedCellsAndAfterGameOver() {
        GameModel model = new GameModel(3);
        assertTrue(model.play(1, 1));
        assertFalse(model.play(1, 1));
        GameModel won = play(3, new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}, new int[]{0, 2});
        assertFalse(won.play(2, 2));
    }

    @Test
    void undoRestoresCellTurnAndResult() {
        GameModel model = play(3, new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}, new int[]{0, 2});
        assertTrue(model.undo());
        assertFalse(model.isOver());
        assertNull(model.getWinningLine());
        assertEquals(GameModel.EMPTY, model.getCell(0, 2));
        assertEquals(GameModel.X, model.getCurrentPlayer());
        assertArrayEquals(new int[]{1, 1, GameModel.O}, model.getLastMove());
    }

    @Test
    void forfeitEndsTheGameWithoutALine() {
        GameModel model = play(10, new int[]{0, 0}, new int[]{5, 5});
        model.forfeit(GameModel.X, EndReason.TIMEOUT);
        assertTrue(model.isOver());
        assertEquals(GameModel.O, model.getWinner());
        assertNull(model.getWinningLine());
        assertEquals(EndReason.TIMEOUT, model.getEndReason());
        assertFalse(model.play(1, 1));
    }

    @Test
    void undoAfterForfeitOnlyCancelsTheForfeit() {
        GameModel model = play(10, new int[]{0, 0}, new int[]{5, 5});
        model.forfeit(GameModel.X, EndReason.RESIGN);
        assertTrue(model.undo());
        assertFalse(model.isOver());
        assertEquals(EndReason.NONE, model.getEndReason());
        assertEquals(2, model.getMoveCount());
        assertEquals(GameModel.X, model.getCurrentPlayer());
    }

    @Test
    void reportsLineAndDrawReasons() {
        GameModel won = play(3, new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}, new int[]{0, 2});
        assertEquals(EndReason.LINE, won.getEndReason());
        GameModel drawn = play(3,
                new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 2}, new int[]{1, 1}, new int[]{1, 0},
                new int[]{1, 2}, new int[]{2, 1}, new int[]{2, 0}, new int[]{2, 2});
        assertEquals(EndReason.DRAW, drawn.getEndReason());
        drawn.forfeit(GameModel.X, EndReason.TIMEOUT);
        assertEquals(EndReason.DRAW, drawn.getEndReason(), "a finished game cannot be forfeited");
    }
}
