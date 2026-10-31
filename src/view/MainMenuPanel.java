package view;

import utils.I18n;
import view.components.AppButton;
import view.components.GradientTitle;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Landing screen: title, the two game modes and secondary actions.
 */
public class MainMenuPanel extends PaperPanel {
    private static final int COLUMN_WIDTH = 320;

    public MainMenuPanel(MainWindow window) {
                setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        GradientTitle title = new GradientTitle("CARO", 84f);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(I18n.t("menu.subtitle"));
        subtitle.setFont(Theme.font(Font.PLAIN, 15f));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        AppButton vsAi = button(I18n.t("menu.vsAi"), AppButton.Variant.PRIMARY);
        vsAi.setToolTipText(I18n.t("menu.vsAi.tooltip"));
        AppButton twoPlayers = button(I18n.t("menu.twoPlayers"), AppButton.Variant.SECONDARY);
        twoPlayers.setToolTipText(I18n.t("menu.twoPlayers.tooltip"));
        AppButton lan = button(I18n.t("menu.lan"), AppButton.Variant.SECONDARY);
        lan.setToolTipText(I18n.t("menu.lan.tooltip"));
        AppButton settings = button(I18n.t("menu.settings"), AppButton.Variant.SECONDARY);
        AppButton about = button(I18n.t("menu.about"), AppButton.Variant.SECONDARY);
        AppButton stats = button(I18n.t("menu.stats"), AppButton.Variant.SECONDARY);
        AppButton quit = button(I18n.t("menu.quit"), AppButton.Variant.GHOST);

        vsAi.addActionListener(e -> window.showScreen(MainWindow.SETUP_AI));
        twoPlayers.addActionListener(e -> window.showScreen(MainWindow.SETUP_TWO));
        settings.addActionListener(e -> window.showScreen(MainWindow.SETTINGS));
        about.addActionListener(e -> window.showScreen(MainWindow.ABOUT));
        stats.addActionListener(e -> window.showScreen(MainWindow.STATS));
        lan.addActionListener(e -> window.showScreen(MainWindow.LAN));
        quit.addActionListener(e -> window.requestQuit());

        column.add(title);
        column.add(subtitle);
        column.add(Box.createVerticalStrut(40));
        column.add(vsAi);
        column.add(Box.createVerticalStrut(12));
        column.add(twoPlayers);
        column.add(Box.createVerticalStrut(12));
        column.add(lan);
        column.add(Box.createVerticalStrut(28));
        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);
        for (AppButton b : new AppButton[]{settings, stats, about}) {
            b.compact();
            row.add(b);
        }
        fix(row, 44);
        column.add(row);
        column.add(Box.createVerticalStrut(12));
        column.add(quit);

        add(column);
    }

    private static AppButton button(String text, AppButton.Variant variant) {
        AppButton b = new AppButton(text, variant);
        if (variant == AppButton.Variant.PRIMARY) b.setFont(Theme.semibold(17f));
        fix(b, variant == AppButton.Variant.PRIMARY ? 54 : 48);
        return b;
    }

    private static void fix(JComponent c, int height) {
        Dimension d = new Dimension(COLUMN_WIDTH, height);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
    }
}
