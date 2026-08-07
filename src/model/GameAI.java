package model;

public class GameAI {
    private int n;
    private int aiPlayer;
    private int humanPlayer;
    private int target;

    public GameAI(int n, int aiPlayer, int humanPlayer) {
        this.n = n;
        this.aiPlayer = aiPlayer;
        this.humanPlayer = humanPlayer;
        this.target = (n <= 5) ? n : 5;
    }

    public int[] getBestMove(int[][] board) {
        long bestScore = -1;
        int[] bestMove = {-1, -1};

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 0) {
                    long attack = evaluateCell(board, i, j, aiPlayer);
                    long defense = evaluateCell(board, i, j, humanPlayer);
                    long totalScore = attack + defense;
                    
                    if (totalScore > bestScore) {
                        bestScore = totalScore;
                        bestMove[0] = i;
                        bestMove[1] = j;
                    } else if (totalScore == bestScore) {
                        if (Math.random() > 0.5) {
                            bestMove[0] = i;
                            bestMove[1] = j;
                        }
                    }
                }
            }
        }
        
        if (bestScore == 0) {
            bestMove[0] = n / 2;
            bestMove[1] = n / 2;
        }

        return bestMove;
    }

    private long evaluateCell(int[][] board, int r, int c, int player) {
        long score = 0;
        int[][] directions = { {0, 1}, {1, 0}, {1, 1}, {1, -1} };
        
        for (int[] dir : directions) {
            int dx = dir[0];
            int dy = dir[1];
            
            int count = 1;
            int blocks = 0;
            
            // Forward
            for (int step = 1; step < target; step++) {
                int nr = r + dx * step;
                int nc = c + dy * step;
                if (nr < 0 || nr >= n || nc < 0 || nc >= n) {
                    blocks++;
                    break;
                }
                if (board[nr][nc] == player) {
                    count++;
                } else if (board[nr][nc] == 0) {
                    break;
                } else {
                    blocks++;
                    break;
                }
            }
            
            // Backward
            for (int step = 1; step < target; step++) {
                int nr = r - dx * step;
                int nc = c - dy * step;
                if (nr < 0 || nr >= n || nc < 0 || nc >= n) {
                    blocks++;
                    break;
                }
                if (board[nr][nc] == player) {
                    count++;
                } else if (board[nr][nc] == 0) {
                    break;
                } else {
                    blocks++;
                    break;
                }
            }
            
            score += getScore(count, blocks, player == aiPlayer);
        }
        return score;
    }
    
    private long getScore(int count, int blocks, boolean isAttack) {
        if (blocks == 2 && count < target) return 0;
        if (count >= target) return isAttack ? 1000000000L : 500000000L;
        
        long base = (long) Math.pow(10, count + 1);
        if (isAttack) base *= 1.2;
        if (blocks == 1) base /= 2;
        
        return base;
    }
}
