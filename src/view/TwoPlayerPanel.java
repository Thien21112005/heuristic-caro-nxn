package view;

import controller.ButtonHoverEffect;
import controller.PlayAction;
import utils.SoundPlayer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TwoPlayerPanel extends JPanel {
    private GameMenu gameMenu;
    private BackgroundPanel backgroundPanel;
    private int n = 3;
    private String selectPath = "/assets/Select.wav";
    private String scrollPath = "/assets/Scroll.wav";

    public TwoPlayerPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel) {
        this.gameMenu = gameMenu;
        this.backgroundPanel = backgroundPanel;
        setLayout(null);

        SoundPlayer sound = new SoundPlayer(selectPath);
        JTextField textField = new JTextField("3");
        textField.setFont(new Font("Arial", Font.PLAIN, 30));
        textField.setEditable(false);
        textField.setBounds(130, 270, 300, 60);

        JButton upButton = new CustomButton("Up");
        JButton downButton = new CustomButton("Down");
        JButton playButton = new CustomButton("Play");
        playButton.setBounds(250, 400, 300, 60);
        upButton.setBounds(450, 270, 100, 60);
        downButton.setBounds(570, 270, 100, 60);

        upButton.addMouseListener(new ButtonHoverEffect(upButton, scrollPath));
        downButton.addMouseListener(new ButtonHoverEffect(downButton, scrollPath));
        upButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sound.playOnce();
                n++;
                textField.setText(String.valueOf(n));
            }
        });

        downButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sound.playOnce();
                if (n > 3) {
                    n--;
                    textField.setText(String.valueOf(n));
                }
            }
        });

        playButton.addMouseListener(new ButtonHoverEffect(playButton, scrollPath));
        playButton.addActionListener(new PlayAction(gameMenu, selectPath));
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameMenu.setGridSize(n);
            }
        });

        JButton backBackButton = new CustomButton("Back");
        backBackButton.setBounds(50, 40, 100, 30);
        backBackButton.addMouseListener(new ButtonHoverEffect(backBackButton, scrollPath));

        backBackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SoundPlayer sound = new SoundPlayer(selectPath);
                sound.playOnce();
                gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "NewGame");
            }
        });

        backgroundPanel.add(textField);
        backgroundPanel.add(upButton);
        backgroundPanel.add(downButton);
        backgroundPanel.add(playButton);
        backgroundPanel.add(backBackButton);
    }
}
