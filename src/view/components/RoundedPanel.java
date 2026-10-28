package view.components;

import view.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Translucent "glass" card with rounded corners and a hairline border.
 */
public class RoundedPanel extends JPanel {
    private final int radius;
    private Color fill = Theme.alpha(Theme.SURFACE, 225);
    private Color border = Theme.alpha(Theme.BORDER, 200);

    public RoundedPanel() {
        this(Theme.RADIUS_LG);
    }

    public RoundedPanel(int radius) {
        this.radius = radius;
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout) {
        this();
        setLayout(layout);
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setBorderColor(Color border) {
        this.border = border;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
