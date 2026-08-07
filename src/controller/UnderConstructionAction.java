package controller;

import utils.SoundPlayer;
import view.GameMenu;
import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UnderConstructionAction implements ActionListener {
    private GameMenu gameMenu;
    private String alertPath = "/assets/Alert.wav";

    public UnderConstructionAction(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SoundPlayer soundPlayer = new SoundPlayer(alertPath);
        soundPlayer.playOnce();
        view.CustomDialog.showDialog(gameMenu,
                "This feature is still being forged in the Cyber-lab!\nPlease check back later.",
                "Under Construction",
                view.CustomDialog.MESSAGE_OPTION);
    }
}
