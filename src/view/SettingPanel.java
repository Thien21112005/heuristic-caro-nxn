package view;

import controller.ButtonHoverEffect;
import utils.SoundPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class SettingPanel extends JPanel {
    private String selectPath = "/assets/Select.wav";
    private String scrollPath = "/assets/Scroll.wav";
    private GameMenu gameMenu;

    public SettingPanel(GameMenu gameMenu, BackgroundPanel backgroundPanel) {
        this.gameMenu = gameMenu;
        setLayout(null);
        setOpaque(false);

        JLabel titleLabel = new JLabel("SETTINGS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        titleLabel.setForeground(Color.CYAN);
        titleLabel.setBounds(200, 100, 400, 60);

        JButton cheatModeBtn = new CustomButton("Enable Cheat Mode (Undo): OFF");
        cheatModeBtn.setBounds(200, 220, 400, 60);
        cheatModeBtn.addMouseListener(new ButtonHoverEffect(cheatModeBtn, scrollPath));
        
        cheatModeBtn.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            boolean newState = !gameMenu.isCheatModeEnabled();
            gameMenu.setCheatModeEnabled(newState);
            cheatModeBtn.setText("Enable Cheat Mode (Undo): " + (newState ? "ON" : "OFF"));
        });

        JButton backButton = new CustomButton("Back");
        backButton.setBounds(50, 40, 100, 30);
        backButton.addMouseListener(new ButtonHoverEffect(backButton, scrollPath));
        backButton.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
        });

        backgroundPanel.add(titleLabel);
        backgroundPanel.add(cheatModeBtn);
        backgroundPanel.add(backButton);
    }
}
