package view;

import controller.ButtonHoverEffect;
import controller.TwoPlayerAction;
import controller.UnderConstructionAction;
import utils.SoundPlayer;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NewGamePanel extends JPanel {
    private GameMenu gameMenu;
    private BackgroundPanel backgroundPanel;
    private String selectPath = "/assets/Select.wav";
    private String scrollPath = "/assets/Scroll.wav";

    public NewGamePanel(GameMenu gameMenu, BackgroundPanel backgroundPanel) {
        this.gameMenu = gameMenu;
        this.backgroundPanel = backgroundPanel;
        setLayout(null);

        JButton onePlayerButton = new CustomButton("VS AI");
        JButton twoPlayerButton = new CustomButton("Two Players");
        JButton backButton = new CustomButton("Back");

        onePlayerButton.setBounds(250, 220, 300, 60);
        twoPlayerButton.setBounds(250, 320, 300, 60);
        backButton.setBounds(50, 40, 100, 30);
        
        onePlayerButton.addActionListener(new UnderConstructionAction(gameMenu));
        twoPlayerButton.addMouseListener(new ButtonHoverEffect(twoPlayerButton, scrollPath));
        twoPlayerButton.addActionListener(new TwoPlayerAction(gameMenu, selectPath));
        backButton.addMouseListener(new ButtonHoverEffect(backButton, scrollPath));

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SoundPlayer sound = new SoundPlayer(selectPath);
                sound.playOnce();
                gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
            }
        });

        backgroundPanel.add(onePlayerButton);
        backgroundPanel.add(twoPlayerButton);
        backgroundPanel.add(backButton);
    }
}
