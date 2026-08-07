package view;

import controller.ButtonHoverEffect;
import utils.SoundPlayer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AboutGamePanel extends JPanel {
    private GameMenu gameMenu;
    private BackgroundPanel backgroundPanel;
    private String selectPath = "/assets/Select.wav";
    private String scrollPath = "/assets/Scroll.wav";

    public AboutGamePanel(GameMenu gameMenu, BackgroundPanel backgroundPanel) {
        this.gameMenu = gameMenu;
        this.backgroundPanel = backgroundPanel;

        setLayout(null);
        JTextArea textArea = new JTextArea(
                "Game developed by Team \"Nhà Chợt Lét\"\n" +
                "Version: 0.0.1\n\n" +
                "This is a Tic Tac Toe game created as part of our academic project.\n" +
                "All rights reserved © 2024 by Team Nhà Chợt Lét.\n\n" +
                "Features:\n" +
                "- Single Player Mode\n" +
                "- Multiplayer Mode\n" +
                "- Sound Effects and Interactive UI\n" +
                "- Optimized for learning and fun\n\n" +
                "Disclaimer:\n" +
                "This software is for educational purposes only.\n" +
                "Any reproduction or distribution of this software is prohibited " +
                "without prior permission from the development team.\n\n" +
                "Thank you for playing!\n") {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };

        textArea.setCaretPosition(0);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 21));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        scrollPane.setBounds(150, 100, 500, 340);
        
        backgroundPanel.add(scrollPane);

        JButton backButton = new CustomButton("Back");
        backButton.setBounds(50, 40, 100, 30);
        backButton.addMouseListener(new ButtonHoverEffect(backButton, scrollPath));
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SoundPlayer sound = new SoundPlayer(selectPath);
                sound.playOnce();
                gameMenu.getCardLayout().show(gameMenu.getCardPanel(), "MainMenu");
            }
        });
        backgroundPanel.add(backButton);
    }
}
