package view.components;

import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Shows focus rings only while the player navigates with the keyboard
 * (like CSS :focus-visible), so mouse users never see stray outlines.
 */
public final class FocusRing {
    private static volatile boolean keyboard;

    private FocusRing() {}

    /** Watches input globally; call once at startup. */
    public static void install() {
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            boolean before = keyboard;
            if (e instanceof KeyEvent k && k.getID() == KeyEvent.KEY_PRESSED
                    && (k.getKeyCode() == KeyEvent.VK_TAB || k.isActionKey())) {
                keyboard = true;
            } else if (e instanceof MouseEvent m && m.getID() == MouseEvent.MOUSE_PRESSED) {
                keyboard = false;
            }
            if (before != keyboard) {
                Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (owner != null) owner.repaint();
            }
        }, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
    }

    public static boolean shouldPaint(JComponent c) {
        return keyboard && c.isFocusOwner();
    }

    /** Accent outline just inside the component's bounds. */
    public static void paint(Graphics2D g2, int x, int y, int w, int h, int arc) {
        g2.setColor(Theme.alpha(Theme.ACCENT_HOVER, 220));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, arc, arc);
    }
}
