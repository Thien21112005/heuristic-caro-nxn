package model.ai;

import model.GameModel;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EasyAiTest {
    private static final int AI = GameModel.O;
    private static final int HUMAN = GameModel.X;

    @Test
    void alwaysTakesAnImmediateWin() {
        for (int seed = 0; seed < 50; seed++) {
            int[][] board = new int[10][10];
            for (int c = 2; c < 6; c++) board[4][c] = AI;
            for (int c = 2; c < 6; c++) board[7][c] = HUMAN;
            int[] move = new EasyAi(new Random(seed)).chooseMove(board, AI);
            assertEquals(4, move[0], "seed " + seed);
            assertTrue(move[1] == 1 || move[1] == 6, "seed " + seed);
        }
    }

    @Test
    void blocksAFourOnlySomeOfTheTime() {
        int blocked = 0;
        int trials = 400;
        for (int seed = 0; seed < trials; seed++) {
            int[][] board = new int[10][10];
            for (int r = 3; r < 7; r++) board[r][5] = HUMAN;
            board[2][5] = AI;
            board[0][0] = AI;
            int[] move = new EasyAi(new Random(seed)).chooseMove(board, AI);
            if (move[0] == 7 && move[1] == 5) blocked++;
        }
        double rate = blocked / (double) trials;
        // Blocks with BLOCK_CHANCE, plus occasionally by accident when playing normally
        assertTrue(rate > 0.5 && rate < 0.9, "block rate was " + rate);
    }

    @RepeatedTest(10)
    void neverPicksAnOccupiedCell() {
        Random random = new Random();
        int n = 3 + random.nextInt(10);
        GameModel model = new GameModel(n);
        EasyAi ai = new EasyAi(new Random(random.nextLong()));
        while (!model.isOver()) {
            int[] move = ai.chooseMove(model.getBoard(), model.getCurrentPlayer());
            assertEquals(GameModel.EMPTY, model.getCell(move[0], move[1]));
            model.play(move[0], move[1]);
        }
    }
}
