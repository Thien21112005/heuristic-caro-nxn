package view;

import utils.I18n;
import model.GameConfig;
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
    public static final String STATS = "Stats";
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

        buildScreens();
        setContentPane(cardPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildScreens() {
        cardPanel.add(new MainMenuPanel(this), MAIN_MENU);
        cardPanel.add(new SetupPanel(this, GameMode.VS_AI), SETUP_AI);
        cardPanel.add(new SetupPanel(this, GameMode.TWO_PLAYERS), SETUP_TWO);
        cardPanel.add(new SettingsPanel(this), SETTINGS);
        cardPanel.add(new AboutPanel(this), ABOUT);
        cardPanel.add(new StatsPanel(this), STATS);
    }

    /**
     * Recreates every screen so they pick up a new language. Only reachable from
     * Settings (via the main menu), so no game is in progress.
     */
    public void rebuildScreens(String showAfter) {
        if (currentGame != null) {
            currentGame.dispose();
            currentGame = null;
        }
        cardPanel.removeAll();
        buildScreens();
        setTitle(I18n.t("app.title"));
        cardPanel.revalidate();
        showScreen(showAfter);
        cardPanel.repaint();
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

    public void startGame(GameConfig config) {
        // Only one board lives at a time; drop the previous one so games don't pile up in memory
        if (currentGame != null) {
            currentGame.dispose();
            cardPanel.remove(currentGame);
        }
        currentGame = new GamePanel(this, config);
        cardPanel.add(currentGame, GAME);
        showScreen(GAME);
    }

    public void requestQuit() {
        int confirm = CustomDialog.confirm(this, I18n.t("menu.quit.title"), I18n.t("menu.quit.message"),
                I18n.t("menu.quit"), I18n.t("common.cancel"), true);
        if (confirm == CustomDialog.YES_OPTION) {
            System.exit(0);
        }
    }
}
