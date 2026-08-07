package controller;

import utils.SoundPlayer;
import javax.swing.*;
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
        if (button.isEnabled()) {
            soundPlayer.playOnce();
        }
    }
}
