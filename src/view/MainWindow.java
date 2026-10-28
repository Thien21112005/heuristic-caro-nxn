package view;

import model.GameMode;
import utils.ResourceUtils;
import utils.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    public static final String MAIN_MENU = "MainMenu";
    public static final String SETUP_AI = "SetupAI";
    public static final String SETUP_TWO = "SetupTwo";
    public static final String SETTINGS = "Settings";
    public static final String ABOUT = "About";
    private static final String GAME = "Game";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private GamePanel currentGame;

    public MainWindow(String title) {
        setTitle(title);
        setSize(1120, 740);
        setMinimumSize(new Dimension(980, 680));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImages(loadIcons());
        SoundManager.startMusic();

        cardPanel.add(new MainMenuPanel(this), MAIN_MENU);
        cardPanel.add(new SetupPanel(this, GameMode.VS_AI), SETUP_AI);
        cardPanel.add(new SetupPanel(this, GameMode.TWO_PLAYERS), SETUP_TWO);
        cardPanel.add(new SettingsPanel(this), SETTINGS);
        cardPanel.add(new AboutPanel(this), ABOUT);

        setContentPane(cardPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private static List<Image> loadIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            icons.add(ResourceUtils.getImageIcon("/assets/icon/icon-" + size + ".png").getImage());
        }
        return icons;
    }

    public void showScreen(String name) {
        cardLayout.show(cardPanel, name);
    }

    public void startGame(GameMode mode, int size) {
        // Only one board lives at a time; drop the previous one so games don't pile up in memory
        if (currentGame != null) {
            currentGame.dispose();
            cardPanel.remove(currentGame);
        }
        currentGame = new GamePanel(this, mode, size);
        cardPanel.add(currentGame, GAME);
        showScreen(GAME);
    }

    public void requestQuit() {
        int confirm = CustomDialog.confirm(this, "Quit the game?", "See you next time!", "Quit", "Cancel", true);
        if (confirm == CustomDialog.YES_OPTION) {
            System.exit(0);
        }
    }
}
