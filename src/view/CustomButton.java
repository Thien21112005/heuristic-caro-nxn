package view;

import javax.swing.*;
import java.awt.*;

public class CustomButton extends JButton {
    public CustomButton(String text) {
        super(text);
        setFont(new Font("Segoe UI", Font.BOLD, 22));
        setForeground(Color.WHITE);
        setBackground(new Color(41, 128, 185)); // Flat blue
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(true);
    }
}
