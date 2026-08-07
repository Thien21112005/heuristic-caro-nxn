package view;

import controller.*;
import utils.SoundPlayer;
import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameMenu extends JFrame {
    private CardLayout cardLayout;
    private SoundPlayer sound;
    private JPanel cardPanel;
    private int gridSize = 3;
    private boolean isCheatModeEnabled = false;

    private String selectPath = "/assets/Select.wav";
    private String scrollPath = "/assets/Scroll.wav";
    private String backgroundPath = "/assets/Background.jpg";

    public GameMenu(String title, String filePath) {
        this.sound = new SoundPlayer(filePath);
        this.setTitle(title);
        this.setSize(800, 600);
        this.setResizable(false);
        this.setUndecorated(true);
        sound.playLoop();

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        BackgroundPanel bgmenu = new BackgroundPanel(backgroundPath);
        BackgroundPanel bgabout = new BackgroundPanel(backgroundPath);
        BackgroundPanel bgnewgame = new BackgroundPanel(backgroundPath);
        BackgroundPanel bgtwoplayer = new BackgroundPanel(backgroundPath);
        BackgroundPanel bgoneplayer = new BackgroundPanel(backgroundPath);
        BackgroundPanel bgnxntwoplayer = new BackgroundPanel(backgroundPath);
        BackgroundPanel bgsetting = new BackgroundPanel(backgroundPath);

        bgmenu.setLayout(null);

        JButton newGameButton = new CustomButton("New Game");
        JButton settingButton = new CustomButton("Setting");
        JButton multiplayerButton = new CustomButton("Multiplayer");
        JButton aboutGameButton = new CustomButton("About Game");
        JButton exitButton = new CustomButton("Exit");

        settingButton.addActionListener(e -> {
            new SoundPlayer(selectPath).playOnce();
            cardLayout.show(cardPanel, "Setting");
        });
        multiplayerButton.addActionListener(new UnderConstructionAction(this));

        newGameButton.setBounds(250, 160, 300, 60);
        settingButton.setBounds(250, 250, 300, 60);
        multiplayerButton.setBounds(250, 340, 300, 60);
        aboutGameButton.setBounds(250, 430, 300, 60);
        exitButton.setBounds(50, 530, 100, 30);

        newGameButton.addMouseListener(new ButtonHoverEffect(newGameButton, scrollPath));
        newGameButton.addActionListener(new NewGameAction(this, selectPath));

        aboutGameButton.addMouseListener(new ButtonHoverEffect(aboutGameButton, scrollPath));
        aboutGameButton.addActionListener(new AboutGameAction(this, selectPath));

        exitButton.addMouseListener(new ButtonHoverEffect(exitButton, scrollPath));
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = CustomDialog.showDialog(GameMenu.this, "Are you sure you want to quit the game?", "Exit Confirmation", CustomDialog.YES_NO_OPTION);
                if (confirm == CustomDialog.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        bgmenu.add(newGameButton);
        bgmenu.add(settingButton);
        bgmenu.add(multiplayerButton);
        bgmenu.add(aboutGameButton);
        bgmenu.add(exitButton);

        AboutGamePanel aboutGamePanel = new AboutGamePanel(this, bgabout);
        NewGamePanel newGamePanel = new NewGamePanel(this, bgnewgame);
        TwoPlayerPanel twoPlayerPanel = new TwoPlayerPanel(this, bgtwoplayer);
        OnePlayerPanel onePlayerPanel = new OnePlayerPanel(this, bgoneplayer);
        SettingPanel settingPanel = new SettingPanel(this, bgsetting);

        cardPanel.add(bgmenu, "MainMenu");
        cardPanel.add(bgabout, "AboutGame");
        cardPanel.add(bgnewgame, "NewGame");
        cardPanel.add(bgtwoplayer, "TwoPlayerPanel");
        cardPanel.add(bgoneplayer, "OnePlayerPanel");
        cardPanel.add(bgnxntwoplayer, "TwoPlayernxnPanel");
        cardPanel.add(bgsetting, "Setting");

        this.add(cardPanel);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
        BackgroundPanel bgnxntwoplayer = new BackgroundPanel(backgroundPath);
        TwoPlayerNxnPanel twoPlayerNxnPanel = new TwoPlayerNxnPanel(this, bgnxntwoplayer, this.gridSize);
        cardPanel.add(bgnxntwoplayer, "TwoPlayernxnPanel");
    }

    public void setGridSizeVS_AI(int gridSize) {
        this.gridSize = gridSize;
        BackgroundPanel bgoneplayernxn = new BackgroundPanel(backgroundPath);
        TwoPlayerNxnPanel nxnPanel = new TwoPlayerNxnPanel(this, bgoneplayernxn, this.gridSize, true);
        cardPanel.add(bgoneplayernxn, "OnePlayerNxnPanel");
    }

    public CardLayout getCardLayout() { return cardLayout; }
    public JPanel getCardPanel() { return cardPanel; }
    
    public boolean isCheatModeEnabled() { return isCheatModeEnabled; }
    public void setCheatModeEnabled(boolean enabled) { this.isCheatModeEnabled = enabled; }
}
