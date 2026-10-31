package view.components;

import utils.I18n;
import model.GameModel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Side panel for one player: the big face, name, tally of wins and a live status.
 * The player to move has their face circled in pen; with a turn clock the circle
 * runs out as the time does.
 */
public class PlayerCard extends JComponent {
    public enum Status { WAITING, TURN, THINKING, WINNER }

    private static final int WIDTH = 168;
    private static final int HEIGHT = 270;
    private static final int RING = 112;
    private static final int FACE = 84;
    private static final int MAX_TALLY = 20;

    private int player;
    private final String name;
    private String tag; // optional prefix for the "Plays X" line, e.g. the AI level
    private Color color;
    private int score;
    private Status status = Status.WAITING;
    private int dots = 0;
    private final Timer dotsTimer;
    private long remainingMs = -1;   // turn clock, -1 when not shown
    private int totalSeconds;

    public PlayerCard(int player, String name) {
        this.player = player;
        this.name = name;
        this.color = player == GameModel.X ? Theme.X_COLOR : Theme.O_COLOR;
        dotsTimer = new Timer(350, e -> {
            dots = (dots + 1) % 4;
            repaint();
        });
    }

    /** Which piece this player has this round (sides can swap between rounds). */
    public void setPiece(int player) {
        if (this.player == player) return;
        this.player = player;
        this.color = player == GameModel.X ? Theme.X_COLOR : Theme.O_COLOR;
        repaint();
    }

    /** Shows the turn clock on the circle around the face; pass -1 to hide it. */
    public void setCountdown(long remainingMs, int totalSeconds) {
        this.remainingMs = remainingMs;
        this.totalSeconds = totalSeconds;
        repaint();
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
        int y0 = Math.max(0, (getHeight() - HEIGHT) / 2);
        boolean active = status == Status.TURN || status == Status.THINKING;

        // Face, circled while it is this player's move
        int rx = (w - RING) / 2;
        int ry = y0 + 4;
        Path2D ring = Sketch.loop(rx, ry, RING, RING, player * 17 + 3);
        boolean clock = active && remainingMs >= 0 && totalSeconds > 0;
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (clock) {
            boolean urgent = remainingMs <= 5_000;
            g2.setColor(Theme.RULE);
            g2.draw(ring);
            g2.setColor(urgent ? Theme.DANGER : color);
            g2.draw(Sketch.partial(ring, Math.min(1f, remainingMs / (totalSeconds * 1000f))));
            String secs = String.valueOf((remainingMs + 999) / 1000);
            g2.setFont(Theme.handBold(Theme.LEAD));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(secs, w - fm.stringWidth(secs) - 2, ry + 18);
        } else if (active) {
            g2.setColor(color);
            g2.draw(ring);
        } else if (status == Status.WINNER) {
            g2.setColor(Theme.DANGER);
            g2.draw(ring);
        }
        Composite solid = g2.getComposite();
        if (!active && status != Status.WINNER) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        }
        g2.drawImage(BoardView.pieceImage(player, FACE), (w - FACE) / 2, ry + (RING - FACE) / 2, null);
        g2.setComposite(solid);

        // Name and side
        int y = ry + RING + 30;
        Font nameFont = Theme.handBold(Theme.H3);
        String shownName = fit(g2, name, nameFont, w - 8);
        if (status == Status.WINNER) marker(g2, shownName, nameFont, w, y);
        drawCentered(g2, shownName, nameFont, Theme.TEXT, w, y);
        y += 22;
        String plays = I18n.t("player.plays", player == GameModel.X ? "X" : "O");
        Font small = Theme.font(Font.PLAIN, Theme.SMALL);
        drawCentered(g2, fit(g2, tag == null ? plays : tag + "  ·  " + plays, small, w - 4), small, Theme.TEXT_MUTED, w, y);

        // Tally of wins, the way scores are kept on paper
        y += 16;
        if (score > 1 && score <= MAX_TALLY) paintTally(g2, w, y);
        y += 44;
        String wins = score + " " + I18n.t(score == 1 ? "player.win" : "player.wins");
        drawCentered(g2, wins, Theme.semibold(Theme.SMALL), score > 0 ? Theme.TEXT : Theme.TEXT_MUTED, w, y);

        // Status
        String line = statusText();
        if (line != null) {
            Font f = Theme.hand(Theme.BODY);
            int baseline = y0 + HEIGHT - 6;
            if (status == Status.TURN) marker(g2, line, f, w, baseline);
            drawCentered(g2, line, f, status == Status.WINNER ? Theme.DANGER : Theme.TEXT, w, baseline);
        }
        g2.dispose();
    }

    /** Four strokes and a slash for every five wins. */
    private void paintTally(Graphics2D g2, int w, int top) {
        int stroke = 7, gap = 12, h = 24;
        int groups = score / 5, rest = score % 5;
        int width = groups * (4 * stroke + gap) + rest * stroke;
        double x = (w - width) / 2.0;
        g2.setColor(Theme.TEXT);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int seed = player * 7;
        for (int i = 0; i < groups; i++) {
            for (int s = 0; s < 4; s++) {
                g2.draw(Sketch.line(x + s * stroke, top, x + s * stroke - 1, top + h, seed++, 1));
            }
            g2.draw(Sketch.line(x - 4, top + h - 3, x + 3 * stroke + 4, top + 3, seed++, 1));
            x += 4 * stroke + gap;
        }
        for (int s = 0; s < rest; s++) {
            g2.draw(Sketch.line(x + s * stroke, top, x + s * stroke - 1, top + h, seed++, 1));
        }
    }

    private static void marker(Graphics2D g2, String text, Font font, int w, int baseline) {
        FontMetrics fm = g2.getFontMetrics(font);
        int tw = fm.stringWidth(text);
        int mid = baseline - fm.getAscent() / 3;
        g2.setColor(Theme.WIN);
        g2.setStroke(new BasicStroke(fm.getAscent() + 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(Sketch.line((w - tw) / 2.0, mid, (w + tw) / 2.0, mid, text.length(), 1));
    }

    private static String fit(Graphics2D g2, String text, Font font, int width) {
        FontMetrics fm = g2.getFontMetrics(font);
        if (fm.stringWidth(text) <= width) return text;
        String cut = text;
        while (!cut.isEmpty() && fm.stringWidth(cut + "…") > width) cut = cut.substring(0, cut.length() - 1);
        return cut + "…";
    }

    private String statusText() {
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
