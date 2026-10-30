package view;

import model.GameModel;
import model.Settings;
import utils.I18n;
import view.components.AppButton;
import view.components.BoardView;
import view.components.RoundedPanel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Three-page introduction shown on first launch (and from About): how to win,
 * the ways to play, and the handy controls. Enter / → next, ← back, Esc skips.
 */
public class WalkthroughDialog extends JDialog {
    private static final int PAGES = 3;
    private static final int CARD_WIDTH = 520;

    private int page;
    private final Illustration art = new Illustration();
    private final JLabel title = new JLabel();
    private final JLabel body = new JLabel();
    private final JPanel dots = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
    private final AppButton back = new AppButton(I18n.t("walk.back"), AppButton.Variant.GHOST);
    private final AppButton next = new AppButton("", AppButton.Variant.PRIMARY);

    private WalkthroughDialog(JFrame parent) {
        super(parent, I18n.t("walk.title"), true);
        setUndecorated(true);
        boolean translucent = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        if (translucent) setBackground(new Color(0, 0, 0, 0));

        JPanel backdrop = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(translucent ? Theme.alpha(Theme.BG, 190) : Theme.BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backdrop.setOpaque(false);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 18));
        card.setFill(Theme.SURFACE);
        card.setBorderColor(Theme.alpha(Theme.ACCENT, 140));
        card.setBorder(BorderFactory.createEmptyBorder(28, 36, 24, 36));

        title.setFont(Theme.font(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        body.setFont(Theme.font(Font.PLAIN, 15f));
        body.setForeground(Theme.TEXT_MUTED);
        body.setHorizontalAlignment(SwingConstants.CENTER);
        body.setVerticalAlignment(SwingConstants.TOP);

        JPanel text = new JPanel(new BorderLayout(0, 10));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(body, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(art, BorderLayout.NORTH);
        top.add(text, BorderLayout.CENTER);

        dots.setOpaque(false);
        AppButton skip = new AppButton(I18n.t("walk.skip"), AppButton.Variant.GHOST);
        skip.addActionListener(e -> finish());
        back.addActionListener(e -> show(page - 1));
        next.addActionListener(e -> {
            if (page == PAGES - 1) finish();
            else show(page + 1);
        });
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(skip);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(back);
        right.add(next);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(left, BorderLayout.WEST);
        JPanel dotsHolder = new JPanel(new GridBagLayout()); // centres the dots on the buttons' line
        dotsHolder.setOpaque(false);
        dotsHolder.add(dots);
        footer.add(dotsHolder, BorderLayout.CENTER);
        footer.add(right, BorderLayout.EAST);

        card.add(top, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(CARD_WIDTH, 380));
        backdrop.add(card);
        setContentPane(backdrop);

        getRootPane().setDefaultButton(next);
        key(KeyEvent.VK_ESCAPE, this::finish);
        key(KeyEvent.VK_RIGHT, next::doClick);
        key(KeyEvent.VK_LEFT, () -> {
            if (page > 0) show(page - 1);
        });

        if (parent != null && parent.isShowing()) setBounds(parent.getBounds());
        else {
            pack();
            setLocationRelativeTo(parent);
        }
        show(0);
    }

    /** Shows the walkthrough once on a fresh install. */
    public static void showIfFirstLaunch(JFrame parent) {
        if (Settings.get().isWalkthroughDone()) return;
        open(parent);
    }

    public static void open(JFrame parent) {
        new WalkthroughDialog(parent).setVisible(true);
    }

    private void show(int index) {
        page = Math.max(0, Math.min(PAGES - 1, index));
        title.setText(I18n.t("walk." + page + ".title"));
        body.setText("<html><div style='text-align:center;width:330px'>" + I18n.t("walk." + page + ".body") + "</div></html>");
        art.page = page;
        art.repaint();
        back.setVisible(page > 0);
        next.setText(I18n.t(page == PAGES - 1 ? "walk.done" : "walk.next"));
        dots.removeAll();
        for (int i = 0; i < PAGES; i++) dots.add(new Dot(i == page));
        dots.revalidate();
        dots.repaint();
    }

    private void finish() {
        Settings.get().setWalkthroughDone(true);
        dispose();
    }

    private void key(int code, Runnable action) {
        getRootPane().registerKeyboardAction(e -> action.run(), KeyStroke.getKeyStroke(code, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Page indicator. */
    private static final class Dot extends JComponent {
        private final boolean active;

        Dot(boolean active) {
            this.active = active;
            setPreferredSize(new Dimension(active ? 22 : 8, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(active ? Theme.ACCENT : Theme.BORDER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
        }
    }

    /** Small drawing above each page: a winning line, the three modes, or keycaps. */
    private static final class Illustration extends JComponent {
        int page;

        Illustration() {
            setPreferredSize(new Dimension(CARD_WIDTH - 72, 120));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            switch (page) {
                case 0 -> drawWinningRow(g2, w, h);
                case 1 -> drawModes(g2, w, h);
                default -> drawKeys(g2, w, h);
            }
            g2.dispose();
        }

        private void drawWinningRow(Graphics2D g2, int w, int h) {
            int cell = 54, n = 5;
            int x0 = (w - cell * n) / 2, y0 = (h - cell) / 2;
            g2.setColor(new Color(0x2D3366));
            g2.fillRoundRect(x0 - 8, y0 - 8, cell * n + 16, cell + 16, 16, 16);
            for (int i = 0; i < n; i++) {
                g2.drawImage(BoardView.pieceImage(GameModel.X, cell - 12), x0 + i * cell + 6, y0 + 6, null);
            }
            g2.setColor(Theme.alpha(Theme.WIN, 90));
            g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x0 + cell / 2, y0 + cell / 2, x0 + cell * n - cell / 2, y0 + cell / 2);
            g2.setColor(Theme.WIN);
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x0 + cell / 2, y0 + cell / 2, x0 + cell * n - cell / 2, y0 + cell / 2);
        }

        private void drawModes(Graphics2D g2, int w, int h) {
            String[] labels = {I18n.t("walk.mode.ai"), I18n.t("walk.mode.two"), I18n.t("walk.mode.lan")};
            int[][] pieces = { {GameModel.X, GameModel.O}, {GameModel.X, GameModel.X}, {GameModel.O, GameModel.X} };
            int box = 120, gap = 18;
            int x0 = (w - (box * 3 + gap * 2)) / 2;
            g2.setFont(Theme.semibold(13f));
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < 3; i++) {
                int x = x0 + i * (box + gap);
                g2.setColor(Theme.SURFACE_ALT);
                g2.fillRoundRect(x, 4, box, h - 8, 16, 16);
                g2.drawImage(BoardView.pieceImage(pieces[i][0], 40), x + box / 2 - 42, 18, null);
                g2.drawImage(BoardView.pieceImage(pieces[i][1], 40), x + box / 2 + 2, 18, null);
                g2.setColor(Theme.TEXT);
                g2.drawString(labels[i], x + (box - fm.stringWidth(labels[i])) / 2, h - 22);
            }
        }

        private void drawKeys(Graphics2D g2, int w, int h) {
            String[] keys = {"H", "R", "N", "F11", "Ctrl+Z"};
            g2.setFont(Theme.semibold(16f));
            FontMetrics fm = g2.getFontMetrics();
            int gap = 12, keyH = 46;
            int total = -gap;
            for (String k : keys) total += Math.max(keyH, fm.stringWidth(k) + 26) + gap;
            int x = (w - total) / 2, y = (h - keyH) / 2;
            for (String k : keys) {
                int kw = Math.max(keyH, fm.stringWidth(k) + 26);
                g2.setColor(Theme.BORDER);
                g2.fillRoundRect(x, y + 4, kw, keyH, 12, 12);
                g2.setColor(Theme.SURFACE_ALT);
                g2.fillRoundRect(x, y, kw, keyH, 12, 12);
                g2.setColor(Theme.TEXT);
                g2.drawString(k, x + (kw - fm.stringWidth(k)) / 2, y + (keyH - fm.getHeight()) / 2 + fm.getAscent());
                x += kw + gap;
            }
        }
    }
}
