package view.components;

import utils.I18n;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.function.IntConsumer;

/**
 * Transport controls for stepping through a finished game:
 * first / previous / play-pause / next / last, a scrubber and "move k of n".
 */
public class ReplayBar extends RoundedPanel {
    private final AppButton first = iconButton(Glyph.FIRST);
    private final AppButton prev = iconButton(Glyph.PREV);
    private final AppButton play = iconButton(Glyph.PLAY);
    private final AppButton next = iconButton(Glyph.NEXT);
    private final AppButton last = iconButton(Glyph.LAST);
    private final JSlider slider = new JSlider(0, 0, 0);
    private final JLabel position = new JLabel();
    private boolean adjusting;
    private int total;

    private IntConsumer onSeek = k -> {};
    private Runnable onTogglePlay = () -> {};

    public ReplayBar() {
        super(Theme.RADIUS_LG);
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 18));

        first.setToolTipText(I18n.t("replay.first"));
        prev.setToolTipText(I18n.t("replay.prev"));
        play.setToolTipText(I18n.t("replay.play"));
        next.setToolTipText(I18n.t("replay.next"));
        last.setToolTipText(I18n.t("replay.last"));
        first.addActionListener(e -> onSeek.accept(0));
        prev.addActionListener(e -> onSeek.accept(slider.getValue() - 1));
        next.addActionListener(e -> onSeek.accept(slider.getValue() + 1));
        last.addActionListener(e -> onSeek.accept(total));
        play.addActionListener(e -> onTogglePlay.run());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.setOpaque(false);
        for (AppButton b : new AppButton[]{first, prev, play, next, last}) buttons.add(b);

        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.addChangeListener(e -> {
            if (!adjusting) onSeek.accept(slider.getValue());
        });

        position.setFont(Theme.semibold(13f));
        position.setForeground(Theme.TEXT_MUTED);

        add(buttons, BorderLayout.WEST);
        add(slider, BorderLayout.CENTER);
        add(position, BorderLayout.EAST);
    }

    public void setOnSeek(IntConsumer onSeek) { this.onSeek = onSeek; }
    public void setOnTogglePlay(Runnable onTogglePlay) { this.onTogglePlay = onTogglePlay; }

    public void setRange(int total) {
        this.total = total;
        adjusting = true;
        slider.setMaximum(total);
        adjusting = false;
    }

    public void setPosition(int k) {
        adjusting = true;
        slider.setValue(k);
        adjusting = false;
        position.setText(I18n.t("replay.position", k, total));
        first.setEnabled(k > 0);
        prev.setEnabled(k > 0);
        next.setEnabled(k < total);
        last.setEnabled(k < total);
    }

    public void setPlaying(boolean playing) {
        play.setIcon(new GlyphIcon(playing ? Glyph.PAUSE : Glyph.PLAY));
        play.setToolTipText(I18n.t(playing ? "replay.pause" : "replay.play"));
    }

    private static AppButton iconButton(Glyph glyph) {
        AppButton b = new AppButton("", AppButton.Variant.GHOST).compact();
        b.setIcon(new GlyphIcon(glyph));
        b.setPreferredSize(new Dimension(40, 40));
        return b;
    }

    private enum Glyph { FIRST, PREV, PLAY, PAUSE, NEXT, LAST }

    /** Media-control glyphs drawn as vector shapes so they never depend on font coverage. */
    private static final class GlyphIcon implements Icon {
        private static final int SIZE = 16;
        private final Glyph glyph;

        GlyphIcon(Glyph glyph) {
            this.glyph = glyph;
        }

        @Override
        public int getIconWidth() { return SIZE; }

        @Override
        public int getIconHeight() { return SIZE; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.translate(x, y);
            g2.setColor(c.isEnabled() ? Theme.TEXT : Theme.alpha(Theme.TEXT_MUTED, 120));
            switch (glyph) {
                case PLAY -> g2.fill(triangle(4, 2, 10, true));
                case PAUSE -> {
                    g2.fillRoundRect(3, 2, 4, 12, 2, 2);
                    g2.fillRoundRect(9, 2, 4, 12, 2, 2);
                }
                case NEXT -> g2.fill(triangle(4, 3, 9, true, 10));
                case PREV -> g2.fill(triangle(12, 3, 9, false, 10));
                case LAST -> {
                    g2.fill(triangle(2, 2, 9, true));
                    g2.fillRoundRect(11, 2, 3, 12, 2, 2);
                }
                case FIRST -> {
                    g2.fillRoundRect(2, 2, 3, 12, 2, 2);
                    g2.fill(triangle(14, 2, 9, false));
                }
            }
            g2.dispose();
        }

        /** Triangle pointing right (or left) with its flat side at {@code x}. */
        private static Shape triangle(int x, int top, int width, boolean right) {
            return triangle(x, top, width, right, 12);
        }

        private static Shape triangle(int x, int top, int width, boolean right, int height) {
            Path2D p = new Path2D.Float();
            int tip = right ? x + width : x - width;
            p.moveTo(x, top);
            p.lineTo(tip, top + height / 2f);
            p.lineTo(x, top + height);
            p.closePath();
            return p;
        }
    }
}
