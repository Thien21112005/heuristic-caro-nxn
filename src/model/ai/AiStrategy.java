package model.ai;

/**
 * A move picker. Implementations read the board and never modify it.
 */
public interface AiStrategy {
    /**
     * @param board n×n board of GameModel.EMPTY / X / O
     * @param me    the player to move
     * @return {row, col} of an empty cell
     */
    int[] chooseMove(int[][] board, int me);
}
