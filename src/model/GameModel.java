package model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Board state and rules. Boards up to 5x5 need a full line to win,
 * larger boards need 5 in a row.
 */
public class GameModel {
    public static final int EMPTY = 0;
    public static final int X = 1;
    public static final int O = 2;

    private static final int[][] DIRECTIONS = { {0, 1}, {1, 0}, {1, 1}, {1, -1} };

    private final int n;
    private final int target;
    private final int[][] board;
    private final Deque<int[]> moves = new ArrayDeque<>(); // {row, col, player}

    private int currentPlayer = X;
    private int winner = EMPTY;
    private boolean draw = false;
    private int[] winningLine = null; // [startRow, startCol, endRow, endCol]; null for forfeits
    private EndReason endReason = EndReason.NONE;

    public GameModel(int n) {
        this.n = n;
        this.target = winLengthFor(n);
        this.board = new int[n][n];
    }

    public static int winLengthFor(int n) {
        return n <= 5 ? n : 5;
    }

    public int getSize() { return n; }
    public int getTarget() { return target; }
    public int[][] getBoard() { return board; }
    public int getCell(int r, int c) { return board[r][c]; }
    public int getCurrentPlayer() { return currentPlayer; }
    public int getWinner() { return winner; }
    public boolean isDraw() { return draw; }
    public EndReason getEndReason() { return endReason; }
    public boolean isOver() { return winner != EMPTY || draw; }
    public int[] getWinningLine() { return winningLine; }
    public int getMoveCount() { return moves.size(); }
    public boolean canUndo() { return !moves.isEmpty(); }

    public int movesBy(int player) {
        int count = 0;
        for (int[] m : moves) if (m[2] == player) count++;
        return count;
    }

    /** Every move so far as {row, col, player}, oldest first. */
    public java.util.List<int[]> getMoves() {
        java.util.List<int[]> list = new java.util.ArrayList<>(moves.size());
        java.util.Iterator<int[]> it = moves.descendingIterator();
        while (it.hasNext()) list.add(it.next().clone());
        return list;
    }

    /**
     * Rebuilds the position after the first {@code count} moves of a game,
     * e.g. to step through a finished game.
     */
    public static GameModel replay(int size, java.util.List<int[]> moves, int count) {
        GameModel model = new GameModel(size);
        int k = Math.max(0, Math.min(count, moves.size()));
        for (int i = 0; i < k; i++) {
            int[] m = moves.get(i);
            if (!model.play(m[0], m[1])) {
                throw new IllegalArgumentException("move " + i + " is not legal: " + m[0] + "," + m[1]);
            }
        }
        return model;
    }

    /** Last move as {row, col, player}, or null on an empty board. */
    public int[] getLastMove() { return moves.peek(); }

    public static int opponent(int player) {
        return player == X ? O : X;
    }

    /** Places the current player's piece. Returns false if the move is not allowed. */
    public boolean play(int r, int c) {
        if (isOver() || r < 0 || r >= n || c < 0 || c >= n || board[r][c] != EMPTY) return false;
        board[r][c] = currentPlayer;
        moves.push(new int[]{r, c, currentPlayer});
        if (checkWin(r, c, currentPlayer)) {
            winner = currentPlayer;
            endReason = EndReason.LINE;
        } else if (moves.size() == n * n) {
            draw = true;
            endReason = EndReason.DRAW;
        } else {
            currentPlayer = opponent(currentPlayer);
        }
        return true;
    }

    /**
     * Ends the game with {@code loser} losing without a line (timeout, resignation,
     * disconnect). Does nothing if the game is already over.
     */
    public void forfeit(int loser, EndReason reason) {
        if (isOver()) return;
        winner = opponent(loser);
        winningLine = null;
        endReason = reason;
    }

    /**
     * Takes back the last move; it becomes that player's turn again.
     * After a forfeit, the first undo only cancels the forfeit and keeps the board.
     */
    public boolean undo() {
        if (endReason == EndReason.TIMEOUT || endReason == EndReason.RESIGN) {
            winner = EMPTY;
            endReason = EndReason.NONE;
            return true;
        }
        if (moves.isEmpty()) return false;
        int[] last = moves.pop();
        board[last[0]][last[1]] = EMPTY;
        currentPlayer = last[2];
        winner = EMPTY;
        draw = false;
        winningLine = null;
        endReason = EndReason.NONE;
        return true;
    }

    public void reset() {
        moves.clear();
        for (int[] row : board) java.util.Arrays.fill(row, EMPTY);
        currentPlayer = X;
        winner = EMPTY;
        draw = false;
        winningLine = null;
        endReason = EndReason.NONE;
    }

    private boolean checkWin(int r, int c, int player) {
        for (int[] dir : DIRECTIONS) {
            int count = 1;
            int startR = r, startC = c, endR = r, endC = c;

            for (int step = 1; step < target; step++) {
                int nr = r + dir[0] * step, nc = c + dir[1] * step;
                if (!inside(nr, nc) || board[nr][nc] != player) break;
                count++;
                endR = nr;
                endC = nc;
            }
            for (int step = 1; step < target; step++) {
                int nr = r - dir[0] * step, nc = c - dir[1] * step;
                if (!inside(nr, nc) || board[nr][nc] != player) break;
                count++;
                startR = nr;
                startC = nc;
            }
            if (count >= target) {
                winningLine = new int[]{startR, startC, endR, endC};
                return true;
            }
        }
        return false;
    }

    private boolean inside(int r, int c) {
        return r >= 0 && r < n && c >= 0 && c < n;
    }
}
