package view.components;

import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Small activity indicator: a pen going round a little circle while something
 * is being looked for. Runs only while it is on screen.
 */
public class PenSpinner extends JComponent {
    private static final int SIZE = 18;

    private final Timer timer = new Timer(40, e -> {
        if (isShowing()) repaint();
    });

    public PenSpinner() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int y = (getHeight() - SIZE) / 2;
        Path2D circle = Sketch.loop(2, y + 2, SIZE - 4, SIZE - 4, 3);
        float cycle = (System.currentTimeMillis() % 1400) / 1400f;
        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.rotate(cycle * Math.PI * 2, SIZE / 2.0, y + SIZE / 2.0);
        g2.draw(Sketch.partial(circle, 0.7f));
        g2.dispose();
    }
}
