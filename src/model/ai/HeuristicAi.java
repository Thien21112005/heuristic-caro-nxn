package model.ai;

import model.GameModel;

import java.util.Random;

/**
 * The original single-ply engine (Normal difficulty): every empty cell gets
 * attack + defense points from 4-direction ray casting and the best cell wins.
 */
public class HeuristicAi implements AiStrategy {
    private static final int[][] DIRECTIONS = { {0, 1}, {1, 0}, {1, 1}, {1, -1} };

    private final Random random;

    public HeuristicAi() {
        this(new Random());
    }

    public HeuristicAi(Random random) {
        this.random = random;
    }

    @Override
    public int[] chooseMove(int[][] board, int me) {
        int n = board.length;
        long bestScore = -1;
        int[] bestMove = {-1, -1};
        int ties = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] != GameModel.EMPTY) continue;
                long total = score(board, i, j, me);
                if (total > bestScore) {
                    bestScore = total;
                    bestMove[0] = i;
                    bestMove[1] = j;
                    ties = 1;
                } else if (total == bestScore && random.nextInt(++ties) == 0) {
                    // Reservoir sampling keeps every tied cell equally likely
                    bestMove[0] = i;
                    bestMove[1] = j;
                }
            }
        }

        if (bestScore == 0 && board[n / 2][n / 2] == GameModel.EMPTY) {
            bestMove[0] = n / 2;
            bestMove[1] = n / 2;
        }
        return bestMove;
    }

    /** Attack value for {@code me} plus defense value against the opponent at an empty cell. */
    public long score(int[][] board, int r, int c, int me) {
        return evaluateCell(board, r, c, me, true) + evaluateCell(board, r, c, GameModel.opponent(me), false);
    }

    private static long evaluateCell(int[][] board, int r, int c, int player, boolean isAttack) {
        int n = board.length;
        int target = GameModel.winLengthFor(n);
        long score = 0;

        for (int[] dir : DIRECTIONS) {
            int count = 1;
            int blocks = 0;
            for (int sign = -1; sign <= 1; sign += 2) {
                for (int step = 1; step < target; step++) {
                    int nr = r + sign * dir[0] * step;
                    int nc = c + sign * dir[1] * step;
                    if (nr < 0 || nr >= n || nc < 0 || nc >= n) {
                        blocks++;
                        break;
                    }
                    if (board[nr][nc] == player) {
                        count++;
                    } else {
                        if (board[nr][nc] != GameModel.EMPTY) blocks++;
                        break;
                    }
                }
            }
            score += lineScore(count, blocks, target, isAttack);
        }
        return score;
    }

    private static long lineScore(int count, int blocks, int target, boolean isAttack) {
        if (count >= target) return isAttack ? 1_000_000_000L : 500_000_000L;
        if (blocks == 2) return 0;

        long base = (long) Math.pow(10, count + 1);
        if (isAttack) base = base * 6 / 5;
        if (blocks == 1) base /= 2;
        return base;
    }
}
