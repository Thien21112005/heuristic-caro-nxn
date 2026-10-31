package view;

import model.GameMode;
import model.Stats;
import model.ai.Difficulty;
import utils.I18n;
import view.components.AppButton;
import view.components.CenteringScroll;
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
public class StatsPanel extends PaperPanel {
    private static final int TILE_WIDTH = 360;
    private static final int TILE_HEIGHT = 168;

    private final Tile[] tiles = {
            new Tile(Stats.bucket(GameMode.VS_AI, Difficulty.EASY), "difficulty.easy", false),
            new Tile(Stats.bucket(GameMode.VS_AI, Difficulty.NORMAL), "difficulty.normal", false),
            new Tile(Stats.bucket(GameMode.VS_AI, Difficulty.HARD), "difficulty.hard", false),
            new Tile(Stats.bucket(GameMode.TWO_PLAYERS, Difficulty.NORMAL), "mode.twoPlayers", true),
    };

    public StatsPanel(MainWindow window) {
                setLayout(new BorderLayout());
        Runnable back = () -> window.showScreen(MainWindow.MAIN_MENU);
        add(TopBar.withBack(back), BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(22, 32, 14, 32));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(I18n.t("stats.title"));
        title.setFont(Theme.handBold(Theme.H2));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel(I18n.t("stats.subtitle"));
        subtitle.setFont(Theme.font(Font.PLAIN, Theme.BODY));
        subtitle.setForeground(Theme.TEXT_MUTED);
        header.add(title);
                header.add(subtitle);

        JPanel grid = new JPanel(new GridLayout(2, 2, 32, 14));
        grid.setOpaque(false);
        for (Tile tile : tiles) grid.add(tile);

        AppButton reset = new AppButton(I18n.t("stats.reset"), AppButton.Variant.GHOST);
        reset.setAccent(Theme.DANGER);
        reset.addActionListener(e -> {
            int option = CustomDialog.confirm(window, I18n.t("stats.reset.title"), I18n.t("stats.reset.message"),
                    I18n.t("stats.reset.confirm"), I18n.t("common.cancel"), true);
            if (option == CustomDialog.YES_OPTION) {
                Stats.get().reset();
                refresh();
            }
        });
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(reset);

        card.add(header, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        // A scroll pane instead of GridBagLayout: when space runs short, GridBagLayout drops to the
        // card's minimum size and squashes the tiles flat
        add(CenteringScroll.wrap(card), BorderLayout.CENTER);

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
    private static final class Tile extends JPanel {
        private final String bucket;
        private final String titleKey;
        private final boolean twoPlayers;
        private Stats.Entry entry = Stats.Entry.EMPTY;

        Tile(String bucket, String titleKey, boolean twoPlayers) {
            setOpaque(false);
            this.bucket = bucket;
            this.titleKey = titleKey;
            this.twoPlayers = twoPlayers;
            setPreferredSize(new Dimension(TILE_WIDTH, TILE_HEIGHT));
        }

        void load() {
            entry = Stats.get().get(bucket);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int pad = 0;
            int top = 4;
            int w = getWidth();

            g2.setFont(Theme.hand(Theme.LEAD));
            g2.setColor(Theme.TEXT);
            g2.drawString(I18n.t(titleKey), pad, top + 16);
            String games = I18n.t("stats.games", entry.played());
            g2.setFont(Theme.font(Font.PLAIN, Theme.SMALL));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(games, w - pad - g2.getFontMetrics().stringWidth(games), top + 16);

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
            g2.setFont(Theme.handBold(Theme.H1));
            g2.setColor(entry.played() == 0 ? Theme.TEXT_MUTED : Theme.TEXT);
            g2.drawString(big, pad, top + 60);
            int bigWidth = g2.getFontMetrics().stringWidth(big);
            g2.setFont(Theme.font(Font.PLAIN, Theme.SMALL));
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(caption, pad + bigWidth + 12, top + 58);

            // Win / draw / loss bar
            int barY = top + 72;
            int barW = w - 2 * pad;
            int barH = 10;
            g2.setColor(Theme.RULE);
            g2.fillRoundRect(pad, barY, barW, barH, barH, barH);
            if (entry.played() > 0) {
                Shape clip = g2.getClip();
                g2.clip(new java.awt.geom.RoundRectangle2D.Float(pad, barY, barW, barH, barH, barH));
                int x = pad;
                int[] parts = {entry.wins(), entry.draws(), entry.losses()};
                Color[] colors = {Theme.ACCENT, Theme.BORDER, Theme.DANGER};
                for (int i = 0; i < parts.length; i++) {
                    int pw = Math.round(barW * parts[i] / (float) entry.played());
                    if (i == parts.length - 1) pw = pad + barW - x;
                    g2.setColor(colors[i]);
                    g2.fillRect(x, barY, pw, barH);
                    x += pw;
                }
                g2.setClip(clip);
            }

            g2.setFont(Theme.font(Font.PLAIN, Theme.SMALL));
            int y = barY + barH + 20;
            String wdl = twoPlayers
                    ? I18n.t("stats.twoPlayers.line", entry.wins(), entry.draws(), entry.losses())
                    : I18n.t("stats.wdl", entry.wins(), entry.draws(), entry.losses());
            g2.setColor(Theme.TEXT);
            g2.drawString(wdl, pad, y);

            if (!twoPlayers) {
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(I18n.t("stats.streak", entry.currentStreak(), entry.bestStreak()), pad, y + 20);
                if (entry.fastestWin() > 0) g2.drawString(I18n.t("stats.fastest", entry.fastestWin()), pad, y + 40);
            }
            g2.dispose();
        }
    }
}
