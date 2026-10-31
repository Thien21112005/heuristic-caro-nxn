package view.components;

import model.GameModel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * A small finished game drawn on paper: the same faces, highlighter and red loop
 * as the real board. Used as the picture on the main menu and in the walkthrough.
 */
public class MiniBoard extends JComponent {
    private final int[][] cells;      // rows of GameModel.X / O / EMPTY
    private final int[] win;          // {row1, col1, row2, col2}, or null

    public MiniBoard(int[][] cells, int[] win) {
        this.cells = cells;
        this.win = win;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int rows = cells.length, cols = cells[0].length;
        // Room around the sheet, because the pen loop may run a little past its edge
        int cell = Math.min((getWidth() - 32) / cols, (getHeight() - 32) / rows);
        if (cell <= 0) {
            g2.dispose();
            return;
        }
        int x0 = (getWidth() - cell * cols) / 2;
        int y0 = (getHeight() - cell * rows) / 2;

        Path2D sheet = Sketch.box(x0, y0, cell * cols, cell * rows, Theme.RADIUS, rows * 11 + cols);
        g2.setColor(Theme.SURFACE);
        g2.fill(sheet);
        if (win != null) {
            BoardView.paintHighlight(g2, x0 + (win[1] + .5f) * cell, y0 + (win[0] + .5f) * cell,
                    x0 + (win[3] + .5f) * cell, y0 + (win[2] + .5f) * cell, cell, 5);
        }
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int c = 1; c < cols; c++) {
            g2.draw(Sketch.line(x0 + c * cell, y0 + 1, x0 + c * cell, y0 + rows * cell - 1, c * 13, 1));
        }
        for (int r = 1; r < rows; r++) {
            g2.draw(Sketch.line(x0 + 1, y0 + r * cell, x0 + cols * cell - 1, y0 + r * cell, r * 29, 1));
        }
        g2.setColor(Theme.TEXT_MUTED);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(sheet);
        if (win != null) {
            BoardView.paintWinLoop(g2, x0 + (win[1] + .5f) * cell, y0 + (win[0] + .5f) * cell,
                    x0 + (win[3] + .5f) * cell, y0 + (win[2] + .5f) * cell, cell, 5, 1f);
        }
        int inset = Math.max(1, Math.round(cell * 0.06f));
        int face = cell - 2 * inset;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (cells[r][c] != GameModel.EMPTY) {
                    g2.drawImage(BoardView.pieceImage(cells[r][c], face), x0 + c * cell + inset, y0 + r * cell + inset, null);
                }
            }
        }
        g2.dispose();
    }
}
