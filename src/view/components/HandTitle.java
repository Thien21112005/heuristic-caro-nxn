package view.components;

import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * A big handwritten title with a quick violet pen underline.
 */
public class HandTitle extends JComponent {
    private static final int UNDERLINE_ROOM = 18;

    private final String text;
    private final Font font;

    public HandTitle(String text, float size) {
        this.text = text;
        this.font = Theme.handBold(size);
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(font);
        return new Dimension(fm.stringWidth(text) + 8, fm.getAscent() + fm.getDescent() / 2 + UNDERLINE_ROOM);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int baseline = fm.getAscent();
        g2.setColor(Theme.TEXT);
        g2.drawString(text, 0, baseline);

        int w = fm.stringWidth(text);
        double y = baseline + fm.getDescent() / 2.0 + 4;
        Path2D stroke = Sketch.line(2, y, w + 4, y - 3, text.hashCode(), 2.2);
        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(stroke);
        g2.dispose();
    }
}
