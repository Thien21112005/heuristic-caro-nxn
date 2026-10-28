package model.ai;

import model.GameModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Incremental board evaluator built on "windows": every run of {@code target}
 * consecutive cells along a row, column or diagonal. A window that holds only
 * one player's stones is worth more the fuller it is, so gapped shapes such as
 * XX_X or XX_XX are recognised as naturally as solid ones. Placing or removing
 * a stone only touches the ~20 windows through that cell.
 *
 * Cells are addressed by flat index {@code row * n + col}.
 */
final class PatternEvaluator {
    static final long WIN = 1_000_000_000L;

    final int n;
    final int target;
    private final int[] cells;
    private final int[][] windows;      // window id -> cell indices
    private final int[][] cellWindows;  // cell index -> window ids
    private final int[][] count;        // [player][window] stones of that player
    private final long[] weight;        // weight[k] for a clean window holding k stones
    private final long[] score = new long[3];
    private final int[] threats = new int[3]; // windows one stone away from winning, per player
    private final int[] near;           // stones within Chebyshev distance 2 of each cell
    private int completed;              // windows filled by one player (someone has won)
    private int stones;

    PatternEvaluator(int[][] board) {
        this.n = board.length;
        this.target = GameModel.winLengthFor(n);
        this.cells = new int[n * n];
        this.count = new int[3][];
        this.near = new int[n * n];

        weight = new long[target + 1];
        for (int k = 1; k < target; k++) weight[k] = (long) Math.pow(10, k - 1);
        weight[target] = 0; // a finished line is tracked through `completed`, not the score

        List<int[]> list = new ArrayList<>();
        for (int[] d : BoardUtil.DIRECTIONS) {
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    int er = r + d[0] * (target - 1), ec = c + d[1] * (target - 1);
                    if (er < 0 || er >= n || ec < 0 || ec >= n) continue;
                    int[] w = new int[target];
                    for (int k = 0; k < target; k++) w[k] = (r + d[0] * k) * n + (c + d[1] * k);
                    list.add(w);
                }
            }
        }
        windows = list.toArray(new int[0][]);
        count[1] = new int[windows.length];
        count[2] = new int[windows.length];

        int[] perCell = new int[n * n];
        for (int[] w : windows) for (int idx : w) perCell[idx]++;
        cellWindows = new int[n * n][];
        for (int i = 0; i < n * n; i++) cellWindows[i] = new int[perCell[i]];
        int[] fill = new int[n * n];
        for (int id = 0; id < windows.length; id++) for (int idx : windows[id]) cellWindows[idx][fill[idx]++] = id;

        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                if (board[r][c] != GameModel.EMPTY) place(r * n + c, board[r][c]);
    }

    int cell(int idx) { return cells[idx]; }
    int stoneCount() { return stones; }
    boolean isFull() { return stones == n * n; }
    boolean isWon() { return completed > 0; }
    boolean hasThreat(int player) { return threats[player] > 0; }
    boolean isNearStones(int idx) { return near[idx] > 0; }

    /** Static value from {@code side}'s point of view. */
    long evaluate(int side) {
        return score[side] - score[GameModel.opponent(side)];
    }

    void place(int idx, int player) {
        update(idx, player, +1);
        cells[idx] = player;
        stones++;
        touchNeighbours(idx, +1);
    }

    void remove(int idx) {
        int player = cells[idx];
        cells[idx] = GameModel.EMPTY;
        stones--;
        update(idx, player, -1);
        touchNeighbours(idx, -1);
    }

    private void update(int idx, int player, int delta) {
        for (int w : cellWindows[idx]) {
            unscore(w);
            count[player][w] += delta;
            rescore(w);
        }
    }

    private void unscore(int w) {
        int x = count[GameModel.X][w], o = count[GameModel.O][w];
        if (o == 0) {
            score[GameModel.X] -= weight[x];
            if (x == target - 1) threats[GameModel.X]--;
            if (x == target) completed--;
        }
        if (x == 0) {
            score[GameModel.O] -= weight[o];
            if (o == target - 1) threats[GameModel.O]--;
            if (o == target) completed--;
        }
    }

    private void rescore(int w) {
        int x = count[GameModel.X][w], o = count[GameModel.O][w];
        if (o == 0) {
            score[GameModel.X] += weight[x];
            if (x == target - 1) threats[GameModel.X]++;
            if (x == target) completed++;
        }
        if (x == 0) {
            score[GameModel.O] += weight[o];
            if (o == target - 1) threats[GameModel.O]++;
            if (o == target) completed++;
        }
    }

    private void touchNeighbours(int idx, int delta) {
        int r = idx / n, c = idx % n;
        for (int dr = -2; dr <= 2; dr++) {
            int rr = r + dr;
            if (rr < 0 || rr >= n) continue;
            for (int dc = -2; dc <= 2; dc++) {
                int cc = c + dc;
                if (cc >= 0 && cc < n) near[rr * n + cc] += delta;
            }
        }
    }

    /** Empty cells where {@code player} would complete a line right now. */
    List<Integer> winningCells(int player) {
        List<Integer> out = new ArrayList<>(2);
        if (threats[player] == 0) return out;
        int opp = GameModel.opponent(player);
        for (int w = 0; w < windows.length; w++) {
            if (count[player][w] != target - 1 || count[opp][w] != 0) continue;
            for (int idx : windows[w]) {
                if (cells[idx] == GameModel.EMPTY && !out.contains(idx)) out.add(idx);
            }
        }
        return out;
    }

    /**
     * How useful an empty cell is for {@code side}: the score it adds to its own
     * windows plus the score it takes away from the opponent's. Used for move ordering.
     */
    long cellValue(int idx, int side) {
        int opp = GameModel.opponent(side);
        long value = 0;
        for (int w : cellWindows[idx]) {
            int mine = count[side][w], theirs = count[opp][w];
            if (theirs == 0) value += gain(mine) * 11 / 10; // slight preference for attack
            if (mine == 0) value += gain(theirs);
        }
        return value;
    }

    private long gain(int k) {
        if (k + 1 >= target) return WIN / 1000; // completes or blocks a line
        return weight[k + 1] - weight[k];
    }
}
