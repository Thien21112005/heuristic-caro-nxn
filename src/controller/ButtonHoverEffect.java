package controller;

import utils.SoundPlayer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ButtonHoverEffect extends MouseAdapter {
    private JButton button;
    private SoundPlayer soundPlayer;

    public ButtonHoverEffect(JButton button, String soundFilePath) {
        this.button = button;
        this.soundPlayer = new SoundPlayer(soundFilePath);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        button.setBackground(new Color(255, 204, 0));
        button.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
        button.setFont(new Font("Arial", Font.BOLD, 22));
        soundPlayer.playOnce();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        button.setBackground(new Color(0, 102, 204));
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 4));
        button.setFont(new Font("Arial", Font.BOLD, 20));
    }
}
