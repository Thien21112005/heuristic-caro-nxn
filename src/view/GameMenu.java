package view;

import model.GameMode;
import model.Settings;
import utils.SoundManager;

import javax.swing.*;
import java.awt.*;

public class GameMenu extends JFrame {
    public static final String MAIN_MENU = "MainMenu";
    public static final String SETUP_AI = "OnePlayerPanel";
    public static final String SETUP_TWO = "TwoPlayerPanel";
    public static final String SETTINGS = "Setting";
    public static final String ABOUT = "AboutGame";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private JPanel currentGamePanel;

    private static final String BACKGROUND_PATH = "/assets/Background.jpg";

    public GameMenu(String title, String musicPath) {
        setTitle(title);
        setSize(800, 600);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        SoundManager.startMusic();

        BackgroundPanel bgabout = new BackgroundPanel(BACKGROUND_PATH);

        new AboutGamePanel(this, bgabout);

        cardPanel.add(new MainMenuPanel(this), MAIN_MENU);
        cardPanel.add(bgabout, ABOUT);
        cardPanel.add(new SetupPanel(this, GameMode.TWO_PLAYERS), SETUP_TWO);
        cardPanel.add(new SetupPanel(this, GameMode.VS_AI), SETUP_AI);
        cardPanel.add(new SettingsPanel(this), SETTINGS);

        setContentPane(cardPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void showScreen(String name) {
        cardLayout.show(cardPanel, name);
    }

    public void requestQuit() {
        int confirm = CustomDialog.confirm(this, "Quit the game?", "See you next time!", "Quit", "Cancel", true);
        if (confirm == CustomDialog.YES_OPTION) {
            System.exit(0);
        }
    }

    public void startGame(GameMode mode, int size) {
        if (mode == GameMode.VS_AI) {
            setGridSizeVS_AI(size);
            showScreen("OnePlayerNxnPanel");
        } else {
            setGridSize(size);
            showScreen("TwoPlayernxnPanel");
        }
    }

    public void setGridSize(int gridSize) {
        BackgroundPanel bgnxntwoplayer = new BackgroundPanel(BACKGROUND_PATH);
        new TwoPlayerNxnPanel(this, bgnxntwoplayer, gridSize);
        replaceGamePanel(bgnxntwoplayer, "TwoPlayernxnPanel");
    }

    public void setGridSizeVS_AI(int gridSize) {
        BackgroundPanel bgoneplayernxn = new BackgroundPanel(BACKGROUND_PATH);
        new TwoPlayerNxnPanel(this, bgoneplayernxn, gridSize, true);
        replaceGamePanel(bgoneplayernxn, "OnePlayerNxnPanel");
    }

    // Only one game board lives at a time; drop the previous one so boards don't pile up in memory
    private void replaceGamePanel(JPanel panel, String name) {
        if (currentGamePanel != null) {
            cardPanel.remove(currentGamePanel);
        }
        currentGamePanel = panel;
        cardPanel.add(panel, name);
    }

    public CardLayout getCardLayout() { return cardLayout; }
    public JPanel getCardPanel() { return cardPanel; }

    public boolean isCheatModeEnabled() { return Settings.get().isUndoEnabled(); }
    public void setCheatModeEnabled(boolean enabled) { Settings.get().setUndoEnabled(enabled); }
}
