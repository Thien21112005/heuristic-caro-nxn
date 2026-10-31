package view;

import utils.I18n;
import view.components.AppButton;
import view.components.CenteringScroll;
import view.components.RoundedPanel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Credits plus a short "how to play" so new players know the rules and controls.
 */
public class AboutPanel extends PaperPanel {
    public static final String VERSION = "2.0.0";
    private static final int CARD_WIDTH = 560;

    public AboutPanel(MainWindow window) {
                setLayout(new BorderLayout());
        Runnable back = () -> window.showScreen(MainWindow.MAIN_MENU);
        add(TopBar.withBack(back), BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 32, 22, 32));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        JLabel title = new JLabel(I18n.t("about.title"));
        title.setFont(Theme.handBold(Theme.H2));
        title.setForeground(Theme.TEXT);
        header.add(title);
        header.add(Box.createHorizontalStrut(12));
        header.add(version("v" + VERSION));
        left(header);
        card.add(header);

        card.add(paragraph(I18n.t("about.intro"), 10));

        card.add(section(I18n.t("about.howToPlay")));
        card.add(paragraph(I18n.t("about.rules"), 0));

        card.add(section(I18n.t("about.controls")));
        card.add(paragraph(I18n.t("about.controls.body"), 0));

        AppButton tour = new AppButton(I18n.t("about.walkthrough"), AppButton.Variant.SECONDARY).compact();
        tour.addActionListener(e -> WalkthroughDialog.open(window));
        JPanel tourRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tourRow.setOpaque(false);
        tourRow.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        tourRow.add(tour);
        card.add(left(tourRow));

        JLabel team = new JLabel(I18n.t("about.team"));
        team.setFont(Theme.font(Font.PLAIN, Theme.SMALL));
        team.setForeground(Theme.TEXT_MUTED);
        team.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        left(team);
        card.add(team);

        card.setPreferredSize(new Dimension(CARD_WIDTH, card.getPreferredSize().height));
        add(CenteringScroll.wrap(card), BorderLayout.CENTER);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override
            public boolean isEnabled() { return isShowing(); }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { back.run(); }
        });
    }

    private static JComponent section(String text) {
        JLabel label = SetupPanel.sectionLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(16, 0, 4, 0));
        return left(label);
    }

    private static JComponent paragraph(String html, int gapTop) {
        JLabel label = new JLabel("<html><div style='width:370px'>" + html + "</div></html>");
        label.setFont(Theme.font(Font.PLAIN, Theme.BODY));
        label.setForeground(Theme.TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(gapTop, 0, 0, 0));
        return left(label);
    }

    private static JComponent version(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.semibold(Theme.SMALL));
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }
}
