package model;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameAITest {
    private static final int AI = GameModel.O;
    private static final int HUMAN = GameModel.X;

    @Test
    void takesImmediateWin() {
        int[][] board = new int[10][10];
        for (int c = 2; c < 6; c++) board[4][c] = AI;
        for (int c = 2; c < 6; c++) board[7][c] = HUMAN;
        int[] move = new GameAI(10, AI, HUMAN).getBestMove(board);
        assertEquals(4, move[0]);
        assertTrue(move[1] == 1 || move[1] == 6);
    }

    @Test
    void blocksOpponentFourInARow() {
        int[][] board = new int[10][10];
        board[0][0] = HUMAN;
        for (int r = 3; r < 7; r++) board[r][5] = HUMAN;
        board[2][5] = AI;
        int[] move = new GameAI(10, AI, HUMAN).getBestMove(board);
        assertArrayEquals(new int[]{7, 5}, move);
    }

    @Test
    void neverPicksOccupiedCellWhenEveryCellScoresZero() {
        // 4x4 board where the only empty cell sits in fully blocked lines; the center is taken
        int[][] board = {
                {HUMAN, AI, HUMAN, AI},
                {AI, HUMAN, AI, HUMAN},
                {AI, HUMAN, AI, HUMAN},
                {HUMAN, AI, HUMAN, 0},
        };
        int[] move = new GameAI(4, AI, HUMAN).getBestMove(board);
        assertArrayEquals(new int[]{3, 3}, move);
    }

    @RepeatedTest(20)
    void alwaysReturnsAnEmptyCellDuringRandomGames() {
        Random random = new Random();
        int n = 3 + random.nextInt(8);
        GameModel model = new GameModel(n);
        GameAI ai = new GameAI(n, AI, HUMAN);
        while (!model.isOver()) {
            if (model.getCurrentPlayer() == HUMAN) {
                int r, c;
                do {
                    r = random.nextInt(n);
                    c = random.nextInt(n);
                } while (model.getCell(r, c) != GameModel.EMPTY);
                model.play(r, c);
            } else {
                int[] move = ai.getBestMove(model.getBoard());
                assertEquals(GameModel.EMPTY, model.getCell(move[0], move[1]), "AI chose an occupied cell");
                model.play(move[0], move[1]);
            }
        }
    }
}
