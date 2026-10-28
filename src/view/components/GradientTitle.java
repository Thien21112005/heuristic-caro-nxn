package view.components;

import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.font.GlyphVector;

/**
 * Large title text filled with the X → accent → O gradient and a soft glow.
 */
public class GradientTitle extends JComponent {
    private final String text;
    private final Font font;

    public GradientTitle(String text, float size) {
        this.text = text;
        this.font = Theme.font(Font.BOLD, size);
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(font);
        return new Dimension(fm.stringWidth(text) + 40, fm.getAscent() + fm.getDescent() + 16);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        FontMetrics fm = g2.getFontMetrics(font);
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

        GlyphVector gv = font.createGlyphVector(g2.getFontRenderContext(), text);
        Shape shape = gv.getOutline(x, y);

        // Glow: a few widening translucent strokes behind the text
        for (int i = 4; i >= 1; i--) {
            g2.setColor(Theme.alpha(Theme.ACCENT, 14));
            g2.setStroke(new BasicStroke(i * 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(shape);
        }

        Rectangle b = shape.getBounds();
        g2.setPaint(new LinearGradientPaint(b.x, 0, b.x + b.width, 0,
                new float[]{0f, 0.5f, 1f},
                new Color[]{Theme.X_COLOR, Theme.ACCENT_HOVER, Theme.O_COLOR}));
        g2.fill(shape);
        g2.dispose();
    }
}
