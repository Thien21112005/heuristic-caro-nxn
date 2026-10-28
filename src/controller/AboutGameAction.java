package controller;

import utils.SoundPlayer;
import view.GameMenu;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AboutGameAction implements ActionListener {
    private GameMenu gameMenu;
    private SoundPlayer soundPlayer;

    public AboutGameAction(GameMenu gameMenu, String soundFilePath) {
        this.gameMenu = gameMenu;
        this.soundPlayer = new SoundPlayer(soundFilePath);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            soundPlayer.playOnce();
            gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "AboutGame");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
