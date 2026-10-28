package view;

import utils.ResourceUtils;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Full-window background: the artwork scaled to cover the panel (no stretching),
 * darkened with a vignette so content on top stays readable.
 */
public class BackgroundPanel extends JPanel {
    private final Image backgroundImage;

    public BackgroundPanel(String imagePath) {
        this.backgroundImage = ResourceUtils.getImageIcon(imagePath).getImage();
        setBackground(Theme.BG);
        setLayout(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();

        int iw = backgroundImage.getWidth(this);
        int ih = backgroundImage.getHeight(this);
        if (iw > 0 && ih > 0) {
            double scale = Math.max((double) w / iw, (double) h / ih);
            int dw = (int) Math.ceil(iw * scale);
            int dh = (int) Math.ceil(ih * scale);
            g2.drawImage(backgroundImage, (w - dw) / 2, (h - dh) / 2, dw, dh, this);
        }

        g2.setColor(Theme.alpha(Theme.BG, 120));
        g2.fillRect(0, 0, w, h);
        float radius = Math.max(w, h) * 0.75f;
        g2.setPaint(new RadialGradientPaint(w / 2f, h / 2f, radius,
                new float[]{0f, 1f},
                new Color[]{Theme.alpha(Theme.BG, 0), Theme.alpha(Theme.BG, 230)}));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}
