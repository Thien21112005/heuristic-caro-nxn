package view;

import controller.ButtonHoverEffect;
import utils.SoundPlayer;

import javax.swing.*;
import java.awt.*;

public class OnePlayerPanel extends JPanel {
    private String selectPath = "/assets/Select.wav";
    private String scrollPath = "/assets/Scroll.wav";

    public OnePlayerPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel) {
        setLayout(null);
        setOpaque(false);

        JTextField textField = new JTextField("3");
        textField.setFont(new Font("Segoe UI", Font.BOLD, 30));
        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setEditable(false);
        textField.setBounds(350, 270, 100, 60);

        JButton downButton = new CustomButton("-");
        JButton upButton = new CustomButton("+");
        JButton playButton = new CustomButton("Play VS AI");
        playButton.setBounds(250, 400, 300, 60);
        downButton.setBounds(230, 270, 100, 60);
        upButton.setBounds(470, 270, 100, 60);

        upButton.addMouseListener(new ButtonHoverEffect(upButton, scrollPath));
        downButton.addMouseListener(new ButtonHoverEffect(downButton, scrollPath));
        playButton.addMouseListener(new ButtonHoverEffect(playButton, scrollPath));

        upButton.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            int currentValue = Integer.parseInt(textField.getText());
            if (currentValue < 20) textField.setText(String.valueOf(currentValue + 1));
        });

        downButton.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            int currentValue = Integer.parseInt(textField.getText());
            if (currentValue > 3) textField.setText(String.valueOf(currentValue - 1));
        });

        playButton.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            int n = Integer.parseInt(textField.getText());
            gameMenu.setGridSizeVS_AI(n);
            gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "OnePlayerNxnPanel");
        });

        JButton backButton = new CustomButton("Back");
        backButton.setBounds(100, 20, 100, 40);
        backButton.addMouseListener(new ButtonHoverEffect(backButton, scrollPath));
        backButton.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "NewGame");
        });

        backgroundPanel.add(textField);
        backgroundPanel.add(upButton);
        backgroundPanel.add(downButton);
        backgroundPanel.add(playButton);
        backgroundPanel.add(backButton);
    }
}
