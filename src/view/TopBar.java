package view;

import utils.I18n;
import view.components.AppButton;
import view.components.Sketch;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Transparent header strip shared by the secondary screens.
 */
public final class TopBar {
    private TopBar() {}

    public static JPanel withBack(Runnable onBack) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 48, 0, 20));
        bar.add(backButton(I18n.t("common.back"), onBack), BorderLayout.WEST);
        return bar;
    }

    public static AppButton backButton(String text, Runnable onBack) {
        AppButton back = new AppButton(text, AppButton.Variant.GHOST).compact();
        back.setIcon(new BackArrow());
        back.setIconTextGap(8);
        back.setToolTipText("Esc");
        back.addActionListener(e -> onBack.run());
        return back;
    }

    /** A little arrow drawn with the pen, so it does not depend on font coverage. */
    private static final class BackArrow implements Icon {
        @Override
        public int getIconWidth() { return 18; }

        @Override
        public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.translate(x, y);
            g2.setColor(c.getForeground());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(Sketch.line(17, 7, 2, 7, 3, 0.8));
            g2.drawPolyline(new int[]{7, 2, 7}, new int[]{2, 7, 12}, 3);
            g2.dispose();
        }
    }
}
