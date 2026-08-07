package view;

import javax.swing.*;
import java.awt.*;

public class AvatarPanel extends JPanel {
    private boolean isActive = false;
    private Color activeColor;
    private String label = "";

    public AvatarPanel(String label, Color activeColor) {
        this.label = label;
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

        if (!isActive) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        } else {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        
        // Draw default Facebook-like profile avatar
        g2d.setColor(new Color(60, 64, 67)); // Dark gray background
        g2d.fillOval(4, 4, w - 8, h - 8);
        
        g2d.setColor(new Color(154, 160, 166)); // Light gray for person
        int headSize = (w - 8) / 3;
        g2d.fillOval(w / 2 - headSize / 2, h / 4, headSize, headSize);
        g2d.fillArc(w / 2 - headSize, h / 2 + 5, headSize * 2, headSize * 2, 0, 180);
        
        // Draw text over it if any
        if (label != null && !label.isEmpty()) {
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
            FontMetrics fm = g2d.getFontMetrics();
            int stringWidth = fm.stringWidth(label);
            int stringAscent = fm.getAscent();
            
            // Draw text background shadow
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect((w - stringWidth) / 2 - 4, (h - stringAscent) / 2 + 10 - stringAscent + 4, stringWidth + 8, stringAscent + 4);
            
            // Draw text
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, (w - stringWidth) / 2, (h - stringAscent) / 2 + 10);
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
