package controller;

import utils.SoundPlayer;
import view.GameMenu;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlayAction implements ActionListener {
    private GameMenu gameMenu;
    private SoundPlayer soundPlayer;

    public PlayAction(GameMenu gameMenu, String soundFilePath) {
        this.gameMenu = gameMenu;
        this.soundPlayer = new SoundPlayer(soundFilePath);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            soundPlayer.playOnce();
            gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "TwoPlayernxnPanel");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
