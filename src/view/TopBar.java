package view;

import view.components.AppButton;

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
        bar.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));
        bar.add(backButton("Back", onBack), BorderLayout.WEST);
        return bar;
    }

    public static AppButton backButton(String text, Runnable onBack) {
        AppButton back = new AppButton("\u2190  " + text, AppButton.Variant.GHOST);
        back.setToolTipText("Esc");
        back.addActionListener(e -> onBack.run());
        return back;
    }
}
