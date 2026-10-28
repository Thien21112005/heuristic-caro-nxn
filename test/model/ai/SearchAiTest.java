package model.ai;

import model.GameModel;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SearchAiTest {
    private static final int AI = GameModel.O;
    private static final int HUMAN = GameModel.X;

    private static SearchAi ai(long budgetMs, long seed) {
        return new SearchAi(budgetMs, new Random(seed));
    }

    @Test
    void blocksAnOpenThreeAtOneOfItsEnds() {
        int[][] board = new int[15][15];
        board[7][5] = HUMAN;
        board[7][6] = HUMAN;
        board[7][7] = HUMAN;
        board[9][9] = AI;
        board[2][12] = AI;
        int[] move = ai(250, 1).chooseMove(board, AI);
        assertEquals(7, move[0], "must block on the row");
        assertTrue(move[1] == 4 || move[1] == 8, "blocking further away still loses: col " + move[1]);
    }

    @Test
    void fillsTheGapOfABrokenFour() {
        int[][] board = new int[12][12];
        board[3][1] = HUMAN;
        board[3][2] = HUMAN;
        board[3][4] = HUMAN;
        board[3][5] = HUMAN;
        board[8][8] = AI;
        board[8][9] = AI;
        assertArrayEquals(new int[]{3, 3}, ai(250, 2).chooseMove(board, AI));
    }

    @Test
    void prefersItsOwnWinOverBlocking() {
        int[][] board = new int[12][12];
        for (int c = 2; c < 6; c++) board[1][c] = HUMAN;
        for (int c = 2; c < 6; c++) board[6][c] = AI;
        int[] move = ai(250, 3).chooseMove(board, AI);
        assertEquals(6, move[0]);
        assertTrue(move[1] == 1 || move[1] == 6);
    }

    @Test
    void turnsAnOpenThreeIntoAnOpenFour() {
        int[][] board = new int[15][15];
        board[5][5] = AI;
        board[5][6] = AI;
        board[5][7] = AI;
        board[10][2] = HUMAN;
        board[12][12] = HUMAN;
        board[0][14] = HUMAN;
        int[] move = ai(250, 4).chooseMove(board, AI);
        assertEquals(5, move[0]);
        assertTrue(move[1] == 4 || move[1] == 8, "expected an open four, got col " + move[1]);
    }

    @Test
    void createsADoubleThreatWhenAvailable() {
        // O can play (7,7) to make a four on the row and an open three on the column at once
        int[][] board = new int[15][15];
        board[7][4] = AI;
        board[7][5] = AI;
        board[7][6] = AI;
        board[7][3] = HUMAN; // row is blocked on the left: playing (7,7) makes a closed four
        board[8][7] = AI;
        board[9][7] = AI;   // column (8,7),(9,7) + (7,7) = open three
        board[1][1] = HUMAN;
        board[13][1] = HUMAN;
        board[1][13] = HUMAN;
        assertArrayEquals(new int[]{7, 7}, ai(400, 5).chooseMove(board, AI));
    }

    @Test
    void answersWithinTheBudgetOnALargeBoard() {
        Random random = new Random(11);
        int[][] board = new int[20][20];
        for (int i = 0; i < 40; i++) {
            int r = 5 + random.nextInt(10), c = 5 + random.nextInt(10);
            if (board[r][c] == GameModel.EMPTY) board[r][c] = (i % 2 == 0) ? HUMAN : AI;
        }
        SearchAi hard = ai(SearchAi.DEFAULT_BUDGET_MS, 6);
        long start = System.nanoTime();
        int[] move = hard.chooseMove(board, AI);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(GameModel.EMPTY, board[move[0]][move[1]]);
        // Generous bound so slower CI machines don't flake; the budget itself is 250 ms
        assertTrue(ms < 800, "took " + ms + " ms");
        assertTrue(hard.lastDepth() >= 2, "searched only depth " + hard.lastDepth());
    }

    @Test
    void neverLosesToTheNormalAiAndWinsMostGames() {
        int hardWins = 0;
        int hardLosses = 0;
        int games = 6;
        for (int g = 0; g < games; g++) {
            Random random = new Random(100 + g);
            SearchAi hard = new SearchAi(60, random);
            HeuristicAi normal = new HeuristicAi(random);
            int hardSide = g % 2 == 0 ? GameModel.X : GameModel.O;
            GameModel model = new GameModel(10);
            model.play(4 + random.nextInt(2), 4 + random.nextInt(2));
            while (!model.isOver()) {
                int p = model.getCurrentPlayer();
                int[] mv = p == hardSide ? hard.chooseMove(model.getBoard(), p) : normal.chooseMove(model.getBoard(), p);
                assertTrue(model.play(mv[0], mv[1]));
            }
            if (model.getWinner() == hardSide) hardWins++;
            else if (model.getWinner() != GameModel.EMPTY) hardLosses++;
        }
        // With the short test budget some 10x10 games fill up and draw; losing is never acceptable
        assertEquals(0, hardLosses, "hard lost to normal");
        assertTrue(hardWins >= games / 2, "hard won only " + hardWins + "/" + games);
    }

    @RepeatedTest(5)
    void neverLosesOnThreeByThreeAgainstRandomPlay() {
        Random random = new Random();
        SearchAi hard = new SearchAi(30, new Random(random.nextLong()));
        GameModel model = new GameModel(3);
        int hardSide = random.nextBoolean() ? GameModel.X : GameModel.O;
        while (!model.isOver()) {
            int p = model.getCurrentPlayer();
            if (p == hardSide) {
                int[] mv = hard.chooseMove(model.getBoard(), p);
                assertTrue(model.play(mv[0], mv[1]), "illegal move");
            } else {
                int r, c;
                do {
                    r = random.nextInt(3);
                    c = random.nextInt(3);
                } while (model.getCell(r, c) != GameModel.EMPTY);
                model.play(r, c);
            }
        }
        assertNotEquals(GameModel.opponent(hardSide), model.getWinner());
    }
}
