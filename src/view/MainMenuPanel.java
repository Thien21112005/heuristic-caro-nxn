package view;

import model.GameConfig;
import model.GameMode;
import model.GameModel;
import model.Settings;
import utils.I18n;
import view.components.AppButton;
import view.components.HandTitle;
import view.components.MiniBoard;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

/**
 * Landing screen: the handwritten title and one solid button that starts a game
 * with the last settings, beside a finished game showing the two faces.
 */
public class MainMenuPanel extends PaperPanel {
    private static final int COLUMN_WIDTH = 340;
    private static final int X = GameModel.X, O = GameModel.O, E = GameModel.EMPTY;
    private static final int[][] SAMPLE = {
            {O, E, E, E, E, E},
            {E, X, O, E, O, E},
            {E, O, X, E, E, E},
            {E, E, O, X, E, E},
            {E, E, E, E, X, O},
            {E, E, O, E, E, X},
    };

    private final AppButton quick = new AppButton("", AppButton.Variant.PRIMARY);

    public MainMenuPanel(MainWindow window) {
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        HandTitle title = new HandTitle(I18n.t("app.title"), Theme.DISPLAY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("<html><div style='width:250px'>" + I18n.t("menu.subtitle") + "</div></html>");
        subtitle.setFont(Theme.font(Font.PLAIN, Theme.BODY));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        quick.setFont(Theme.hand(Theme.LEAD));
        quick.setToolTipText(I18n.t("menu.quick.tooltip"));
        quick.addActionListener(e -> window.startGame(lastGame()));
        fix(quick, 58);

        AppButton custom = secondary(I18n.t("menu.custom"), I18n.t("menu.custom.tooltip"));
        AppButton twoPlayers = secondary(I18n.t("menu.twoPlayers"), I18n.t("menu.twoPlayers.tooltip"));
        AppButton lan = secondary(I18n.t("menu.lan"), I18n.t("menu.lan.tooltip"));
        custom.addActionListener(e -> window.showScreen(MainWindow.SETUP_AI));
        twoPlayers.addActionListener(e -> window.showScreen(MainWindow.SETUP_TWO));
        lan.addActionListener(e -> window.showScreen(MainWindow.LAN));

        AppButton stats = quiet(I18n.t("menu.stats"));
        AppButton settings = quiet(I18n.t("menu.settings"));
        AppButton about = quiet(I18n.t("menu.about"));
        AppButton quit = quiet(I18n.t("menu.quit"));
        stats.addActionListener(e -> window.showScreen(MainWindow.STATS));
        settings.addActionListener(e -> window.showScreen(MainWindow.SETTINGS));
        about.addActionListener(e -> window.showScreen(MainWindow.ABOUT));
        quit.addActionListener(e -> window.requestQuit());
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        links.setOpaque(false);
        for (AppButton b : new AppButton[]{stats, settings, about, quit}) links.add(b);
        links.setAlignmentX(Component.LEFT_ALIGNMENT);
        links.setMaximumSize(new Dimension(COLUMN_WIDTH + 40, 44));

        column.add(title);
        column.add(Box.createVerticalStrut(10));
        column.add(subtitle);
        column.add(Box.createVerticalStrut(28));
        column.add(quick);
        column.add(Box.createVerticalStrut(12));
        column.add(custom);
        column.add(Box.createVerticalStrut(10));
        column.add(twoPlayers);
        column.add(Box.createVerticalStrut(10));
        column.add(lan);
        column.add(Box.createVerticalStrut(20));
        column.add(links);

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(0, 68, 0, 24);
        add(column, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.weightx = 1;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        right.insets = new Insets(36, 8, 36, 32);
        add(new MiniBoard(SAMPLE, new int[]{1, 1, 5, 5}), right);

        // The label names the settings it will use, which change whenever a game is set up
        refreshQuick();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshQuick();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "quick");
        getActionMap().put("quick", new AbstractAction() {
            @Override
            public boolean isEnabled() { return isShowing(); }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { quick.doClick(); }
        });
    }

    private void refreshQuick() {
        Settings s = Settings.get();
        int size = s.getBoardSize(GameMode.VS_AI);
        quick.setText(I18n.t("menu.quick", size, I18n.t("difficulty." + s.getDifficulty().name().toLowerCase())));
    }

    private static GameConfig lastGame() {
        Settings s = Settings.get();
        return GameConfig.vsAi(s.getBoardSize(GameMode.VS_AI), s.getDifficulty(), s.getHumanSide(),
                        s.isAlternateFirst(GameMode.VS_AI))
                .withTurnSeconds(s.getTurnSeconds(GameMode.VS_AI));
    }

    private static AppButton secondary(String text, String tooltip) {
        AppButton b = new AppButton(text, AppButton.Variant.SECONDARY);
        b.setToolTipText(tooltip);
        fix(b, 46);
        return b;
    }

    private static AppButton quiet(String text) {
        AppButton b = new AppButton(text, AppButton.Variant.GHOST).compact();
        b.setFont(Theme.hand(Theme.BODY));
        return b;
    }

    private static void fix(JComponent c, int height) {
        Dimension d = new Dimension(COLUMN_WIDTH, height);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}
