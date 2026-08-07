package model;

import java.util.Stack;

public class GameModel {
    private int n;
    private int[][] array;
    private Stack<int[][]> arrayStack;
    private boolean isXTurn = true;

    public GameModel(int n) {
        this.n = n;
        this.array = new int[n][n];
        this.arrayStack = new Stack<>();
    }

    public int[][] getArray() { return array; }
    public void setArrayValue(int i, int j, int value) { this.array[i][j] = value; }
    
    public boolean isXTurn() { return isXTurn; }
    public void toggleTurn() { this.isXTurn = !this.isXTurn; }
    public void setXTurn(boolean turn) { this.isXTurn = turn; }
    
    public void saveState() {
        int[][] currentArray = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                currentArray[i][j] = array[i][j];
            }
        }
        arrayStack.push(currentArray);
    }
    
    public int[][] undo() {
        if (!arrayStack.isEmpty()) {
            array = arrayStack.pop();
            return array;
        }
        return null;
    }
    
    public boolean canUndo() {
        return !arrayStack.isEmpty();
    }
    
    public void reset() {
        arrayStack.clear();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                array[i][j] = 0;
            }
        }
        isXTurn = true;
    }
    
    public boolean isBoardFull() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (array[i][j] == 0) return false;
            }
        }
        return true;
    }
    
    public boolean checkWin(int i, int j, int value) {
        if (n <= 5) {
            int dem = 0;
            for (int m = 0; m < n; m++) { if (array[m][j] == value) dem++; }
            if (dem == n) return true;
            dem = 0;
            for (int m = 0; m < n; m++) { if (array[i][m] == value) dem++; }
            if (dem == n) return true;
            dem = 0;
            for (int m = 0; m < n; m++) { if (array[m][m] == value) dem++; }
            if (dem == n) return true;
            dem = 0;
            for (int m = 0; m < n; m++) { if (array[m][n - m - 1] == value) dem++; }
            if (dem == n) return true;
            return false;
        } else {
            int[] directions = {-1, 0, 1};
            for (int dx : directions) {
                for (int dy : directions) {
                    if (dx == 0 && dy == 0) continue;
                    int count = 1;
                    for (int step = 1; step < 5; step++) {
                        int ni = i + dx * step;
                        int nj = j + dy * step;
                        if (ni >= 0 && ni < n && nj >= 0 && nj < n && array[ni][nj] == value) {
                            count++;
                        } else {
                            break;
                        }
                    }
                    for (int step = 1; step < 5; step++) {
                        int ni = i - dx * step;
                        int nj = j - dy * step;
                        if (ni >= 0 && ni < n && nj >= 0 && nj < n && array[ni][nj] == value) {
                            count++;
                        } else {
                            break;
                        }
                    }
                    if (count >= 5) return true;
                }
            }
            return false;
        }
    }
}
