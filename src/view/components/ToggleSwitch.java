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
 * iOS-style on/off switch with an animated knob. Space toggles when focused.
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

        Color off = Theme.SURFACE_ALT;
        Color on = Theme.ACCENT;
        g2.setColor(new Color(
                (int) (off.getRed() + (on.getRed() - off.getRed()) * position),
                (int) (off.getGreen() + (on.getGreen() - off.getGreen()) * position),
                (int) (off.getBlue() + (on.getBlue() - off.getBlue()) * position)));
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setColor(Theme.alpha(Color.WHITE, 30));
        g2.drawRoundRect(x, y, w - 1, h - 1, h, h);

        int knob = h - 8;
        int kx = x + 4 + Math.round((w - knob - 8) * position);
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(kx, y + 5, knob, knob);
        g2.setColor(Color.WHITE);
        g2.fillOval(kx, y + 4, knob, knob);

        if (isFocusOwner()) {
            g2.setColor(Theme.alpha(Theme.ACCENT_HOVER, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x - 2, y - 2, w + 3, h + 3, h + 4, h + 4);
        }
        g2.dispose();
    }
}
