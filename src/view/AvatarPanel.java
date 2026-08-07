package view;

import javax.swing.*;
import java.awt.*;
import utils.ResourceUtils;

public class AvatarPanel extends JPanel {
    private ImageIcon image;
    private boolean isActive = false;
    private Color activeColor;

    public AvatarPanel(String imagePath, Color activeColor) {
        this.image = ResourceUtils.getImageIcon(imagePath);
        this.activeColor = activeColor;
        this.setOpaque(false);
    }

    public void setActive(boolean active) {
        this.isActive = active;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (image != null) {
            Image img = image.getImage();
            
            if (!isActive) {
                // Dim the image if inactive
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            } else {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }
            
            g2d.drawImage(img, 4, 4, w - 8, h - 8, this);
        }

        if (isActive) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.setColor(activeColor);
            g2d.setStroke(new BasicStroke(4f));
            g2d.drawRect(2, 2, w - 4, h - 4);
            
            // Outer glow
            g2d.setColor(new Color(activeColor.getRed(), activeColor.getGreen(), activeColor.getBlue(), 100));
            g2d.setStroke(new BasicStroke(8f));
            g2d.drawRect(4, 4, w - 8, h - 8);
        } else {
            g2d.setColor(Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRect(2, 2, w - 4, h - 4);
        }

        g2d.dispose();
    }
}
