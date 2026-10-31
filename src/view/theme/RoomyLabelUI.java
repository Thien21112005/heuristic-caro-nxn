package view.theme;

import com.formdev.flatlaf.ui.FlatLabelUI;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;

/**
 * Labels measure Vietnamese text a few pixels narrower than it is drawn, which
 * used to cut off the last letter whenever a label sat at exactly its preferred
 * size. A little spare width on every label fixes that everywhere at once.
 */
public class RoomyLabelUI extends FlatLabelUI {
    private static final int SPARE_WIDTH = 4;

    protected RoomyLabelUI() {
        super(false);
    }

    public static ComponentUI createUI(JComponent c) {
        return new RoomyLabelUI();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        return d == null ? null : new Dimension(d.width + SPARE_WIDTH, d.height);
    }
}
