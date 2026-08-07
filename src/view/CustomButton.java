package view;

import javax.swing.*;
import java.awt.*;

public class CustomButton extends JButton {
    public CustomButton(String text) {
        super(text);
        setFont(new Font("Arial", Font.BOLD, 20));
        setForeground(Color.WHITE);
        setBackground(new Color(0, 102, 204));
        setFocusPainted(false);
        setBorder(BorderFactory.createLineBorder(Color.WHITE, 4));
        setOpaque(true);
    }
}
