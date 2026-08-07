package controller;

import utils.SoundPlayer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ButtonHoverEffect extends MouseAdapter {
    private JButton button;
    private SoundPlayer soundPlayer;

    private Color normalColor = new Color(41, 128, 185);
    private Color hoverColor = new Color(52, 152, 219);

    public ButtonHoverEffect(JButton button, String soundFilePath) {
        this.button = button;
        this.soundPlayer = new SoundPlayer(soundFilePath);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (button.isEnabled()) {
            button.setBackground(hoverColor);
            soundPlayer.playOnce();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (button.isEnabled()) {
            button.setBackground(normalColor);
        }
    }
}
