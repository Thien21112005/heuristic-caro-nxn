package view;

import model.GameMode;
import model.Stats;
import model.ai.Difficulty;
import utils.I18n;
import view.components.RoundedPanel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

/**
 * Lifetime results: one tile per AI level plus one for two-player games.
 * Reloads every time the screen is shown.
 */
public class StatsPanel extends BackgroundPanel {
    private static final int TILE_WIDTH = 360;
    private static final int TILE_HEIGHT = 206;

    private final Tile[] tiles = {
            new Tile(Stats.bucket(GameMode.VS_AI, Difficulty.EASY), "difficulty.easy", false),
            new Tile(Stats.bucket(GameMode.VS_AI, Difficulty.NORMAL), "difficulty.normal", false),
            new Tile(Stats.bucket(GameMode.VS_AI, Difficulty.HARD), "difficulty.hard", false),
            new Tile(Stats.bucket(GameMode.TWO_PLAYERS, Difficulty.NORMAL), "mode.twoPlayers", true),
    };

    public StatsPanel(MainWindow window) {
        super("/assets/Background.jpg");
        setLayout(new BorderLayout());
        Runnable back = () -> window.showScreen(MainWindow.MAIN_MENU);
        add(TopBar.withBack(back), BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 22));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(I18n.t("stats.title"));
        title.setFont(Theme.font(Font.BOLD, 30f));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel(I18n.t("stats.subtitle"));
        subtitle.setFont(Theme.font(Font.PLAIN, 15f));
        subtitle.setForeground(Theme.TEXT_MUTED);
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);

        JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18));
        grid.setOpaque(false);
        for (Tile tile : tiles) grid.add(tile);

        card.add(header, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);
        add(center, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refresh();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override
            public boolean isEnabled() { return isShowing(); }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { back.run(); }
        });
        refresh();
    }

    private void refresh() {
        for (Tile tile : tiles) tile.load();
    }

    /** One bucket: name, headline number, a W/D/L bar and streak details. */
    private static final class Tile extends RoundedPanel {
        private final String bucket;
        private final String titleKey;
        private final boolean twoPlayers;
        private Stats.Entry entry = Stats.Entry.EMPTY;

        Tile(String bucket, String titleKey, boolean twoPlayers) {
            super(Theme.RADIUS_LG);
            this.bucket = bucket;
            this.titleKey = titleKey;
            this.twoPlayers = twoPlayers;
            setFill(Theme.alpha(Theme.SURFACE_ALT, 230));
            setPreferredSize(new Dimension(TILE_WIDTH, TILE_HEIGHT));
        }

        void load() {
            entry = Stats.get().get(bucket);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int pad = 20;
            int w = getWidth();

            g2.setFont(Theme.semibold(16f));
            g2.setColor(Theme.TEXT);
            g2.drawString(I18n.t(titleKey), pad, pad + 16);
            String games = I18n.t("stats.games", entry.played());
            g2.setFont(Theme.font(Font.PLAIN, 13f));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(games, w - pad - g2.getFontMetrics().stringWidth(games), pad + 16);

            // Headline
            String big;
            String caption;
            if (twoPlayers) {
                big = entry.wins() + " – " + entry.losses();
                caption = I18n.t("stats.twoPlayers.caption");
            } else {
                big = entry.played() == 0 ? "—" : entry.winRatePercent() + "%";
                caption = I18n.t("stats.winRate");
            }
            g2.setFont(Theme.font(Font.BOLD, 40f));
            g2.setColor(entry.played() == 0 ? Theme.TEXT_MUTED : Theme.TEXT);
            g2.drawString(big, pad, pad + 70);
            int bigWidth = g2.getFontMetrics().stringWidth(big);
            g2.setFont(Theme.font(Font.PLAIN, 13f));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(caption, pad + bigWidth + 12, pad + 68);

            // Win / draw / loss bar
            int barY = pad + 88;
            int barW = w - 2 * pad;
            int barH = 10;
            g2.setColor(Theme.alpha(Theme.BORDER, 200));
            g2.fillRoundRect(pad, barY, barW, barH, barH, barH);
            if (entry.played() > 0) {
                Shape clip = g2.getClip();
                g2.clip(new java.awt.geom.RoundRectangle2D.Float(pad, barY, barW, barH, barH, barH));
                int x = pad;
                int[] parts = {entry.wins(), entry.draws(), entry.losses()};
                Color[] colors = {Theme.X_COLOR, Theme.TEXT_MUTED, Theme.O_COLOR};
                for (int i = 0; i < parts.length; i++) {
                    int pw = Math.round(barW * parts[i] / (float) entry.played());
                    if (i == parts.length - 1) pw = pad + barW - x;
                    g2.setColor(colors[i]);
                    g2.fillRect(x, barY, pw, barH);
                    x += pw;
                }
                g2.setClip(clip);
            }

            g2.setFont(Theme.font(Font.PLAIN, 13f));
            int y = barY + barH + 22;
            String wdl = twoPlayers
                    ? I18n.t("stats.twoPlayers.line", entry.wins(), entry.draws(), entry.losses())
                    : I18n.t("stats.wdl", entry.wins(), entry.draws(), entry.losses());
            g2.setColor(Theme.TEXT);
            g2.drawString(wdl, pad, y);

            if (!twoPlayers) {
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(I18n.t("stats.streak", entry.currentStreak(), entry.bestStreak()), pad, y + 22);
                if (entry.fastestWin() > 0) g2.drawString(I18n.t("stats.fastest", entry.fastestWin()), pad, y + 42);
            }
            g2.dispose();
        }
    }
}
