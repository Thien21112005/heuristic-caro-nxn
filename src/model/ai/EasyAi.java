package model.ai;

import model.GameModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Beatable opponent: takes a win when it sees one, but only blocks the
 * player's winning move some of the time and often picks a weaker cell.
 */
public class EasyAi implements AiStrategy {
    static final double BLOCK_CHANCE = 0.6;
    static final double SLOPPY_CHANCE = 0.35;
    static final int SLOPPY_POOL = 6;

    private final Random random;
    private final HeuristicAi heuristic;

    public EasyAi() {
        this(new Random());
    }

    public EasyAi(Random random) {
        this.random = random;
        this.heuristic = new HeuristicAi(random);
    }

    @Override
    public int[] chooseMove(int[][] board, int me) {
        int[] win = BoardUtil.findWinningMove(board, me);
        if (win != null) return win;

        int[] block = BoardUtil.findWinningMove(board, GameModel.opponent(me));
        if (block != null && random.nextDouble() < BLOCK_CHANCE) return block;

        if (random.nextDouble() < SLOPPY_CHANCE) {
            List<long[]> ranked = new ArrayList<>(); // {score, row, col}
            int n = board.length;
            for (int r = 0; r < n; r++)
                for (int c = 0; c < n; c++)
                    if (board[r][c] == GameModel.EMPTY) ranked.add(new long[]{heuristic.score(board, r, c, me), r, c});
            ranked.sort((a, b) -> Long.compare(b[0], a[0]));
            // Skip the cell that would block a win, so a "missed block" stays missed
            ranked.removeIf(m -> block != null && m[1] == block[0] && m[2] == block[1]);
            if (!ranked.isEmpty()) {
                long[] pick = ranked.get(random.nextInt(Math.min(SLOPPY_POOL, ranked.size())));
                return new int[]{(int) pick[1], (int) pick[2]};
            }
        }
        return heuristic.chooseMove(board, me);
    }
}
