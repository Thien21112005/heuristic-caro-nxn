package view.components;

import utils.SoundManager;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * On/off switch with an animated knob. Space toggles when focused.
 */
public class ToggleSwitch extends JComponent {
    private boolean selected;
    private float position;
    private final Timer animator;
    private Consumer<Boolean> onChange = v -> {};

    public ToggleSwitch(boolean selected) {
        this.selected = selected;
        this.position = selected ? 1f : 0f;
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        animator = new Timer(15, e -> {
            float target = this.selected ? 1f : 0f;
            position += (target - position) * 0.3f;
            if (Math.abs(target - position) < 0.02f) {
                position = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle();
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle");
        getActionMap().put("toggle", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggle();
            }
        });
    }

    public void setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
    }

    public boolean isSelected() { return selected; }

    public void toggle() {
        if (!isEnabled()) return;
        selected = !selected;
        SoundManager.play(SoundManager.CLICK);
        animator.start();
        onChange.accept(selected);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(52, 30);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = 52, h = 30;
        int y = (getHeight() - h) / 2;
        int x = getWidth() - w;
        boolean on = position > 0.5f;

        // Off is an empty outlined track, on is a filled one: state reads without colour
        g2.setColor(mix(Theme.SURFACE, isEnabled() ? Theme.ACCENT : Theme.TEXT_DISABLED, position));
        g2.fillRoundRect(x, y, w - 1, h - 1, h, h);
        g2.setColor(isEnabled() ? (on ? Theme.ACCENT : Theme.BORDER) : Theme.TEXT_DISABLED);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(x, y, w - 1, h - 1, h, h);

        int knob = h - 10;
        int kx = x + 5 + Math.round((w - knob - 11) * position);
        g2.setColor(on ? Theme.SURFACE : isEnabled() ? Theme.TEXT_MUTED : Theme.TEXT_DISABLED);
        g2.fillOval(kx, y + 5, knob - 1, knob - 1);

        if (FocusRing.shouldPaint(this)) {
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x - 3, y - 3, w + 5, h + 5, h + 6, h + 6);
        }
        g2.dispose();
    }

    private static Color mix(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
