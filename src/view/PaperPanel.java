package view;

import view.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Full-window background: a page of a Vietnamese school notebook, with its faint
 * printed squares and the pink margin line.
 */
public class PaperPanel extends JPanel {
    private static final int SQUARE = 24;
    private static final int MARGIN_X = 36;

    public PaperPanel() {
        setBackground(Theme.BG);
        setLayout(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(Theme.RULE);
        for (int x = MARGIN_X % SQUARE; x < w; x += SQUARE) g.drawLine(x, 0, x, h);
        for (int y = SQUARE; y < h; y += SQUARE) g.drawLine(0, y, w, y);
        g.setColor(Theme.MARGIN);
        g.drawLine(MARGIN_X, 0, MARGIN_X, h);
        g.drawLine(MARGIN_X + 3, 0, MARGIN_X + 3, h);
    }
}
