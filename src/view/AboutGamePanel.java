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
        JTextArea textArea = new JTextArea();

        textArea.setText("Game developed by Team \"Nhà Chọt Lét\"  \n" +
                "Version: 0.0.1  \n\n" +
                "This is a Tic Tac Toe game created as part of our academic project.  \n" +
                "All rights reserved © 2024 by Team Nhà Chọt Lét.  \n\n" +
                "Features:  \n" +
                "- Single Player Mode  \n" +
                "- Multiplayer Mode  \n" +
                "- Sound Effects and Interactive UI  \n" +
                "- Optimized for learning and fun  \n\n" +
                "Disclaimer:  \n" +
                "This software is for educational purposes only. Redistribution or commercial use is prohibited without prior permission from the development team.  \n\n" +
                "Thank you for playing!\n");

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
        
        JPanel glassPanel = new JPanel();
        glassPanel.setBounds(150, 100, 500, 340);
        glassPanel.setBackground(new Color(0, 0, 0, 160));
        glassPanel.setLayout(new BorderLayout());
        glassPanel.add(scrollPane, BorderLayout.CENTER);
        
        backgroundPanel.add(glassPanel);

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
