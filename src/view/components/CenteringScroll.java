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
        return scrollOver(new Holder(content, false));
    }

    /** Same, but keeps the top edge fixed, for content that grows and must not slide from under the pointer. */
    public static JScrollPane wrapTop(JComponent content) {
        return scrollOver(new Holder(content, true));
    }

    private static JScrollPane scrollOver(Holder holder) {
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    private static final class Holder extends JPanel implements Scrollable {
        Holder(JComponent content, boolean top) {
            super(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(top ? 4 : 8, 16, top ? 6 : 16, 16));
            GridBagConstraints gbc = new GridBagConstraints();
            if (top) {
                gbc.weighty = 1;
                gbc.anchor = GridBagConstraints.NORTH;
            }
            add(content, gbc);
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
