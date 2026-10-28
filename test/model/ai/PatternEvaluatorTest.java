package model.ai;

import model.GameModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PatternEvaluatorTest {
    private static final int X = GameModel.X;
    private static final int O = GameModel.O;

    @Test
    void incrementalScoreMatchesAFreshEvaluator() {
        Random random = new Random(7);
        for (int game = 0; game < 30; game++) {
            int n = 3 + random.nextInt(14);
            int[][] board = new int[n][n];
            PatternEvaluator ev = new PatternEvaluator(board);
            int player = X;
            for (int move = 0; move < n * n / 2; move++) {
                int idx = random.nextInt(n * n);
                if (ev.cell(idx) != GameModel.EMPTY) continue;
                ev.place(idx, player);
                board[idx / n][idx % n] = player;
                player = GameModel.opponent(player);
            }
            PatternEvaluator fresh = new PatternEvaluator(board);
            assertEquals(fresh.evaluate(X), ev.evaluate(X), "n=" + n);
            assertEquals(fresh.isWon(), ev.isWon());
            assertEquals(fresh.hasThreat(X), ev.hasThreat(X));
            assertEquals(fresh.hasThreat(O), ev.hasThreat(O));
        }
    }

    @Test
    void placeThenRemoveRestoresEverything() {
        int[][] board = new int[12][12];
        board[5][5] = X;
        board[5][6] = X;
        board[6][6] = O;
        PatternEvaluator ev = new PatternEvaluator(board);
        long before = ev.evaluate(X);
        for (int idx = 0; idx < 144; idx++) {
            if (ev.cell(idx) != GameModel.EMPTY) continue;
            ev.place(idx, O);
            ev.remove(idx);
            assertEquals(before, ev.evaluate(X), "cell " + idx);
        }
    }

    @Test
    void findsTheGapInABrokenFour() {
        int[][] board = new int[10][10];
        board[3][1] = X;
        board[3][2] = X;
        board[3][4] = X;
        board[3][5] = X; // XX_XX
        PatternEvaluator ev = new PatternEvaluator(board);
        assertTrue(ev.hasThreat(X));
        assertEquals(List.of(3 * 10 + 3), ev.winningCells(X));
        assertFalse(ev.hasThreat(O));
    }

    @Test
    void openFourHasTwoWinningCells() {
        int[][] board = new int[10][10];
        for (int r = 2; r < 6; r++) board[r][4] = O;
        PatternEvaluator ev = new PatternEvaluator(board);
        List<Integer> cells = ev.winningCells(O);
        assertEquals(2, cells.size());
        assertTrue(cells.contains(1 * 10 + 4));
        assertTrue(cells.contains(6 * 10 + 4));
    }

    @Test
    void detectsACompletedLineOnSmallBoards() {
        int[][] board = new int[3][3];
        PatternEvaluator ev = new PatternEvaluator(board);
        ev.place(0, X);
        ev.place(4, X);
        assertFalse(ev.isWon());
        ev.place(8, X);
        assertTrue(ev.isWon());
        ev.remove(8);
        assertFalse(ev.isWon());
    }

    @Test
    void blockedShapesScoreLessThanOpenOnes() {
        int[][] open = new int[11][11];
        open[5][4] = X;
        open[5][5] = X;
        open[5][6] = X;
        int[][] blocked = new int[11][11];
        blocked[5][0] = X;
        blocked[5][1] = X;
        blocked[5][2] = X;
        blocked[5][3] = O;
        assertTrue(new PatternEvaluator(open).evaluate(X) > new PatternEvaluator(blocked).evaluate(X) + 100);
    }
}
