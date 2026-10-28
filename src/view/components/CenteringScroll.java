package view.components;

import javax.swing.*;
import java.awt.*;

/**
 * Centers a component in the available space and only scrolls when the window
 * is too small to show all of it (e.g. a tall setup card at the minimum size).
 */
public final class CenteringScroll {
    private CenteringScroll() {}

    public static JScrollPane wrap(JComponent content) {
        JScrollPane scroll = new JScrollPane(new Holder(content));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    private static final class Holder extends JPanel implements Scrollable {
        Holder(JComponent content) {
            super(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
            add(content);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport && getParent().getWidth() > getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }
}
