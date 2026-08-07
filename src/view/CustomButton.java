package view;

import javax.swing.*;
import java.awt.*;

public class CustomButton extends JButton {
    private Color normalColor = new Color(41, 128, 185, 220);
    private Color hoverColor = new Color(52, 152, 219, 255);

    public CustomButton(String text) {
        super(text);
        setFont(new Font("Segoe UI", Font.BOLD, 20));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorderPainted(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (getModel().isRollover()) {
            g2.setColor(hoverColor);
        } else {
            g2.setColor(normalColor);
        }
        
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        g2.dispose();
        
        super.paintComponent(g);
    }
}
