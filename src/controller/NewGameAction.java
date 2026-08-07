package controller;

import utils.SoundPlayer;
import view.GameMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NewGameAction implements ActionListener {
    private GameMenu gameMenu;
    private SoundPlayer soundPlayer;

    public NewGameAction(GameMenu gameMenu, String soundFilePath) {
        this.gameMenu = gameMenu;
        this.soundPlayer = new SoundPlayer(soundFilePath);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        soundPlayer.playOnce();
        gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "NewGame");
    }
}
