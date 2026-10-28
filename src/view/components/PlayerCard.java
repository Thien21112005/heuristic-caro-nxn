package view.components;

import utils.I18n;
import model.GameModel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Side card for one player: piece, name, win count and a live status chip.
 * The active player's card lights up in their color.
 */
public class PlayerCard extends JComponent {
    public enum Status { WAITING, TURN, THINKING, WINNER }

    private static final int WIDTH = 180;
    private static final int HEIGHT = 260;

    private final int player;
    private final String name;
    private String tag; // optional prefix for the "Plays X" line, e.g. the AI level
    private final Color color;
    private int score;
    private Status status = Status.WAITING;
    private int dots = 0;
    private final Timer dotsTimer;

    public PlayerCard(int player, String name) {
        this.player = player;
        this.name = name;
        this.color = player == GameModel.X ? Theme.X_COLOR : Theme.O_COLOR;
        dotsTimer = new Timer(350, e -> {
            dots = (dots + 1) % 4;
            repaint();
        });
    }

    public void setTag(String tag) {
        this.tag = tag;
        repaint();
    }

    public void setScore(int score) {
        this.score = score;
        repaint();
    }

    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.THINKING) dotsTimer.start();
        else dotsTimer.stop();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(150, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = Math.min(getHeight(), HEIGHT);
        int y0 = (getHeight() - h) / 2;
        boolean active = status == Status.TURN || status == Status.THINKING || status == Status.WINNER;
        Color accent = status == Status.WINNER ? Theme.WIN : color;

        // Card
        if (active) {
            g2.setColor(Theme.alpha(accent, 40));
            g2.fillRoundRect(-2, y0 - 2, w + 3, h + 3, Theme.RADIUS_LG + 4, Theme.RADIUS_LG + 4);
        }
        g2.setColor(Theme.alpha(Theme.SURFACE_ALT, active ? 245 : 215));
        g2.fillRoundRect(0, y0, w - 1, h - 1, Theme.RADIUS_LG, Theme.RADIUS_LG);
        g2.setColor(active ? Theme.alpha(accent, 200) : Theme.alpha(Theme.BORDER, 200));
        g2.setStroke(new BasicStroke(active ? 2f : 1f));
        g2.drawRoundRect(0, y0, w - 1, h - 1, Theme.RADIUS_LG, Theme.RADIUS_LG);

        if (!active) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        }

        // Piece in a tinted circle
        int circle = 76;
        int cx = (w - circle) / 2;
        int cy = y0 + 22;
        g2.setColor(Theme.alpha(color, 30));
        g2.fillOval(cx, cy, circle, circle);
        g2.setColor(Theme.alpha(color, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx, cy, circle, circle);
        int img = 50;
        g2.drawImage(BoardView.pieceImage(player, img), (w - img) / 2, cy + (circle - img) / 2, null);

        // Name and side
        int y = cy + circle + 30;
        drawCentered(g2, name, Theme.font(Font.BOLD, 18f), Theme.TEXT, w, y);
        y += 20;
        String plays = I18n.t("player.plays", player == GameModel.X ? "X" : "O");
        drawCentered(g2, tag == null ? plays : tag + "  ·  " + plays, Theme.font(Font.PLAIN, 13f), Theme.TEXT_MUTED, w, y);

        // Score
        y += 44;
        drawCentered(g2, String.valueOf(score), Theme.font(Font.BOLD, 34f), Theme.TEXT, w, y);
        y += 16;
        drawCentered(g2, I18n.t(score == 1 ? "player.win" : "player.wins"), Theme.semibold(11f), Theme.TEXT_MUTED, w, y);

        // Status chip
        g2.setComposite(AlphaComposite.SrcOver);
        String chip = chipText();
        if (chip != null) {
            Font f = Theme.semibold(12f);
            FontMetrics fm = g2.getFontMetrics(f);
            int chipW = fm.stringWidth(status == Status.THINKING ? I18n.t("player.chip.thinking") + "..." : chip) + 24;
            int chipH = 26;
            int chipX = (w - chipW) / 2;
            int chipY = y0 + h - chipH - 14;
            g2.setColor(Theme.alpha(accent, 50));
            g2.fillRoundRect(chipX, chipY, chipW, chipH, chipH, chipH);
            g2.setColor(accent);
            g2.setFont(f);
            g2.drawString(chip, chipX + 12, chipY + (chipH - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }

    private String chipText() {
        switch (status) {
            case TURN: return I18n.t("player.chip.turn");
            case THINKING: return I18n.t("player.chip.thinking") + ".".repeat(dots);
            case WINNER: return I18n.t("player.chip.winner");
            default: return null;
        }
    }

    private static void drawCentered(Graphics2D g2, String text, Font font, Color color, int width, int baseline) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (width - fm.stringWidth(text)) / 2, baseline);
    }
}
