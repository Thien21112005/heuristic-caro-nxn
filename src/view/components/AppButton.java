package view.components;

import utils.SoundManager;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Rounded, animated button used across the whole app.
 */
public class AppButton extends JButton {
    public enum Variant { PRIMARY, SECONDARY, GHOST, DANGER }

    private Variant variant;
    private float hover = 0f;
    private final Timer animator;
    private boolean sounds = true;
    private Color accent; // ghost buttons: text colour instead of the muted default

    public AppButton(String text) {
        this(text, Variant.PRIMARY);
    }

    public AppButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(Theme.semibold(15f));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 22));

        animator = new Timer(15, e -> {
            float target = getModel().isRollover() && isEnabled() ? 1f : 0f;
            hover += (target - hover) * 0.25f;
            if (Math.abs(target - hover) < 0.02f) {
                hover = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled() && sounds) SoundManager.play(SoundManager.HOVER);
                animator.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                animator.start();
            }
        });
        addActionListener(e -> {
            if (sounds) SoundManager.play(SoundManager.CLICK);
        });
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
        repaint();
    }

    /** Tighter padding for icon-like or chip buttons. */
    public AppButton compact() {
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return this;
    }

    public AppButton setAccent(Color accent) {
        this.accent = accent;
        repaint();
        return this;
    }

    public void setSounds(boolean sounds) {
        this.sounds = sounds;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 44));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();
        boolean pressed = getModel().isArmed() && getModel().isPressed();
        if (!isEnabled()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        }

        int arc = Theme.RADIUS;
        switch (variant) {
            case PRIMARY: {
                Color top = pressed ? Theme.ACCENT_PRESSED : blend(Theme.ACCENT, Theme.ACCENT_HOVER, hover);
                Color bottom = pressed ? Theme.ACCENT_PRESSED : blend(new Color(0x5B3DF5), Theme.ACCENT, hover);
                // soft glow under the button on hover
                if (hover > 0) {
                    g2.setColor(Theme.alpha(Theme.ACCENT, (int) (60 * hover)));
                    g2.fillRoundRect(0, 2, w, h - 1, arc + 4, arc + 4);
                }
                g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                g2.fillRoundRect(0, 0, w, h - 2, arc, arc);
                setForeground(Color.WHITE);
                break;
            }
            case DANGER: {
                Color fill = pressed ? Theme.DANGER.darker() : blend(new Color(0xE11D48), Theme.DANGER, hover);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h - 2, arc, arc);
                setForeground(Color.WHITE);
                break;
            }
            case SECONDARY: {
                Color fill = pressed ? Theme.SURFACE : blend(Theme.SURFACE_ALT, new Color(0x2A2F5E), hover);
                g2.setColor(Theme.alpha(fill, 235));
                g2.fillRoundRect(0, 0, w - 1, h - 3, arc, arc);
                g2.setColor(blend(Theme.BORDER, Theme.ACCENT, hover * 0.7f));
                g2.drawRoundRect(0, 0, w - 1, h - 3, arc, arc);
                setForeground(Theme.TEXT);
                break;
            }
            case GHOST: {
                if (hover > 0) {
                    g2.setColor(Theme.alpha(Color.WHITE, (int) (18 * hover)));
                    g2.fillRoundRect(0, 0, w, h - 2, arc, arc);
                }
                setForeground(accent != null ? blend(accent, accent.brighter(), hover)
                        : blend(Theme.TEXT_MUTED, Theme.TEXT, hover));
                break;
            }
        }
        g2.dispose();

        // Let Swing draw the text (and icon) on top, nudged up to sit inside the shape
        Graphics2D gt = (Graphics2D) g.create();
        gt.translate(0, -1);
        super.paintComponent(gt);
        gt.dispose();
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
