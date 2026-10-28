package model.ai;

import model.GameModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hard difficulty: negamax with alpha-beta pruning and iterative deepening
 * under a time budget, on top of {@link PatternEvaluator}.
 *
 * Two rules keep the tree small enough to see real tactics:
 * a side that can complete a line wins immediately, and a side facing a
 * completed-line threat may only play the blocking cell (two threats = lost).
 */
public class SearchAi implements AiStrategy {
    public static final long DEFAULT_BUDGET_MS = 250;

    private static final long INF = Long.MAX_VALUE / 4;
    private static final long WIN = PatternEvaluator.WIN;
    private static final int ROOT_WIDTH = 16;
    private static final int WIDTH = 10;
    private static final int MAX_DEPTH = 12;

    private final long budgetNanos;
    private final Random random;

    private PatternEvaluator ev;
    private long deadline;
    private long nodes;
    private int lastDepth;

    public SearchAi() {
        this(DEFAULT_BUDGET_MS, new Random());
    }

    public SearchAi(long budgetMs, Random random) {
        this.budgetNanos = budgetMs * 1_000_000L;
        this.random = random;
    }

    /** Deepest fully searched depth of the last call (for tests and tuning). */
    int lastDepth() { return lastDepth; }

    @Override
    public int[] chooseMove(int[][] board, int me) {
        int n = board.length;
        int opp = GameModel.opponent(me);
        ev = new PatternEvaluator(board);
        lastDepth = 0;

        if (ev.stoneCount() == 0) return new int[]{n / 2, n / 2};

        List<Integer> wins = ev.winningCells(me);
        if (!wins.isEmpty()) return toMove(wins.get(0), n);

        List<Integer> root = ev.hasThreat(opp) ? ev.winningCells(opp) : candidates(me, ROOT_WIDTH, true);
        if (root.isEmpty()) return firstEmpty(board);
        if (root.size() == 1) return toMove(root.get(0), n);

        deadline = System.nanoTime() + budgetNanos;
        nodes = 0;
        int best = root.get(0);

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            int iterBest = -1;
            long iterScore = -INF;
            try {
                long alpha = -INF;
                for (int idx : root) {
                    ev.place(idx, me);
                    long v = -negamax(depth - 1, -INF, -alpha, opp, 1);
                    ev.remove(idx);
                    if (v > iterScore) {
                        iterScore = v;
                        iterBest = idx;
                    }
                    if (v > alpha) alpha = v;
                }
            } catch (Timeout t) {
                // Moves searched before the timeout were searched fully; the first one is the
                // previous best, so anything that beat it is a strictly better-informed choice
                if (iterBest != -1) best = iterBest;
                break;
            }
            best = iterBest;
            lastDepth = depth;
            root.remove((Integer) best);
            root.add(0, best);
            if (Math.abs(iterScore) >= WIN - 100) break; // forced win or loss found
            if (depth >= ev.n * ev.n - ev.stoneCount()) break; // searched to the end of the game
        }
        return toMove(best, n);
    }

    private long negamax(int depth, long alpha, long beta, int side, int ply) {
        if ((++nodes & 511) == 0 && System.nanoTime() > deadline) throw Timeout.INSTANCE;

        int opp = GameModel.opponent(side);
        if (ev.hasThreat(side)) return WIN - ply;    // side completes a line on this move
        if (ev.isFull()) return 0;
        if (depth == 0) return ev.evaluate(side);

        List<Integer> moves;
        if (ev.hasThreat(opp)) {
            moves = ev.winningCells(opp);
            if (moves.size() > 1) return -(WIN - ply - 1); // cannot block two lines
        } else {
            moves = candidates(side, WIDTH, false);
            if (moves.isEmpty()) return ev.evaluate(side);
        }

        long best = -INF;
        for (int idx : moves) {
            ev.place(idx, side);
            long v = -negamax(depth - 1, -beta, -alpha, opp, ply + 1);
            ev.remove(idx);
            if (v > best) best = v;
            if (v > alpha) alpha = v;
            if (alpha >= beta) break;
        }
        return best;
    }

    /** Most promising empty cells near existing stones, best first. */
    private List<Integer> candidates(int side, int width, boolean jitter) {
        int size = ev.n * ev.n;
        List<long[]> scored = new ArrayList<>();
        for (int idx = 0; idx < size; idx++) {
            if (ev.cell(idx) != GameModel.EMPTY || !ev.isNearStones(idx)) continue;
            long value = ev.cellValue(idx, side) * 4 + (jitter ? random.nextInt(4) : 0);
            scored.add(new long[]{value, idx});
        }
        scored.sort((a, b) -> Long.compare(b[0], a[0]));
        List<Integer> out = new ArrayList<>(width);
        for (int i = 0; i < scored.size() && i < width; i++) out.add((int) scored.get(i)[1]);
        return out;
    }

    private static int[] toMove(int idx, int n) {
        return new int[]{idx / n, idx % n};
    }

    private static int[] firstEmpty(int[][] board) {
        for (int r = 0; r < board.length; r++)
            for (int c = 0; c < board.length; c++)
                if (board[r][c] == GameModel.EMPTY) return new int[]{r, c};
        return new int[]{-1, -1};
    }

    private static final class Timeout extends RuntimeException {
        static final Timeout INSTANCE = new Timeout();

        private Timeout() {
            super(null, null, false, false);
        }
    }
}
