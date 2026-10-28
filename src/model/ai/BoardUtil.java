package model.ai;

import model.GameModel;

/** Small board helpers shared by the AI strategies. */
final class BoardUtil {
    static final int[][] DIRECTIONS = { {0, 1}, {1, 0}, {1, 1}, {1, -1} };

    private BoardUtil() {}

    /** True if {@code player} placing at the empty cell (r, c) makes a winning line. */
    static boolean completesLine(int[][] board, int r, int c, int player) {
        int n = board.length;
        int target = GameModel.winLengthFor(n);
        for (int[] d : DIRECTIONS) {
            int count = 1;
            for (int sign = -1; sign <= 1; sign += 2) {
                for (int step = 1; step < target; step++) {
                    int nr = r + sign * d[0] * step, nc = c + sign * d[1] * step;
                    if (nr < 0 || nr >= n || nc < 0 || nc >= n || board[nr][nc] != player) break;
                    count++;
                }
            }
            if (count >= target) return true;
        }
        return false;
    }

    /** First empty cell where {@code player} wins immediately, or null. */
    static int[] findWinningMove(int[][] board, int player) {
        for (int r = 0; r < board.length; r++)
            for (int c = 0; c < board.length; c++)
                if (board[r][c] == GameModel.EMPTY && completesLine(board, r, c, player)) return new int[]{r, c};
        return null;
    }
}
