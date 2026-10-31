package view.components;

import utils.SoundManager;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * The button used across the whole app. PRIMARY is the one solid action of a
 * screen; PLAIN and CIRCLED are the two states of an option in a choice row.
 */
public class AppButton extends JButton {
    public enum Variant { PRIMARY, SECONDARY, GHOST, DANGER, PLAIN, CIRCLED }

    private Variant variant;
    private float hover = 0f;
    private final Timer animator;
    private boolean sounds = true;
    private Color accent; // ghost buttons: colour on hover instead of plain ink
    private Path2D shape;
    private int shapeW, shapeH;

    public AppButton(String text) {
        this(text, Variant.PRIMARY);
    }

    public AppButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(Theme.hand(Theme.BODY));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));

        animator = new Timer(15, e -> {
            float target = getModel().isRollover() && isEnabled() ? 1f : 0f;
            hover += (target - hover) * 0.25f;
            if (Math.abs(target - hover) < 0.02f) {
                hover = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) { repaint(); }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
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
        setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
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
        return new Dimension(d.width, Math.max(d.height, 42));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth();
        int h = getHeight();
        boolean pressed = getModel().isArmed() && getModel().isPressed();
        boolean enabled = isEnabled();
        int seed = getText() == null ? 7 : getText().hashCode();
        Path2D box = box(w, h, seed);
        Color focus = Theme.ACCENT;

        switch (variant) {
            case PRIMARY, DANGER -> {
                boolean danger = variant == Variant.DANGER;
                Color base = danger ? Theme.DANGER : Theme.ACCENT;
                Color over = danger ? blend(Theme.DANGER, Color.WHITE, 0.12f) : Theme.ACCENT_HOVER;
                Color down = danger ? Theme.DANGER_PRESSED : Theme.ACCENT_PRESSED;
                g2.setColor(!enabled ? Theme.SURFACE_ALT : pressed ? down : blend(base, over, hover));
                g2.fill(box);
                if (!enabled) {
                    g2.setColor(Theme.TEXT_DISABLED);
                    g2.draw(box);
                }
                setForeground(Theme.SURFACE);
                focus = Theme.SURFACE;
            }
            case SECONDARY -> {
                g2.setColor(!enabled ? Theme.BG : pressed ? Theme.RULE : blend(Theme.SURFACE, Theme.SURFACE_ALT, hover));
                g2.fill(box);
                g2.setColor(enabled ? blend(Theme.BORDER, Theme.TEXT, hover * 0.6f) : Theme.RULE);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(box);
                setForeground(Theme.TEXT);
            }
            case GHOST, PLAIN -> {
                if (pressed || hover > 0) {
                    g2.setColor(pressed ? Theme.RULE : Theme.alpha(Theme.SURFACE_ALT, (int) (255 * hover)));
                    g2.fill(box);
                }
                Color rest = variant == Variant.PLAIN ? Theme.TEXT : Theme.TEXT_MUTED;
                setForeground(accent != null ? blend(rest, accent, hover) : blend(rest, Theme.TEXT, hover));
            }
            case CIRCLED -> {
                g2.setColor(enabled ? Theme.ACCENT : Theme.TEXT_DISABLED);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(Sketch.loop(4, 3, w - 8, h - 6, seed));
                setForeground(Theme.ACCENT);
            }
        }
        if (FocusRing.shouldPaint(this)) FocusRing.paint(g2, 3, 3, w - 6, h - 6, Theme.RADIUS - 2, focus);
        g2.dispose();
        super.paintComponent(g);
    }

    private Path2D box(int w, int h, int seed) {
        if (shape == null || w != shapeW || h != shapeH) {
            shape = Sketch.box(1, 1, w - 2, h - 2, Theme.RADIUS, seed);
            shapeW = w;
            shapeH = h;
        }
        return shape;
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
