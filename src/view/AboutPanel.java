package view;

import view.components.RoundedPanel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Credits plus a short "how to play" so new players know the rules and controls.
 */
public class AboutPanel extends BackgroundPanel {
    public static final String VERSION = "1.0.0";
    private static final int CARD_WIDTH = 560;

    public AboutPanel(GameMenu window) {
        super("/assets/Background.jpg");
        setLayout(new BorderLayout());
        Runnable back = () -> window.showScreen(GameMenu.MAIN_MENU);
        add(TopBar.withBack(back), BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 28, 36));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("About Caro");
        title.setFont(Theme.font(Font.BOLD, 28f));
        title.setForeground(Theme.TEXT);
        header.add(title);
        header.add(Box.createHorizontalStrut(12));
        header.add(pill("v" + VERSION));
        left(header);
        card.add(header);

        card.add(paragraph("A Gomoku / Tic-Tac-Toe game for any board from 3×3 to 20×20, "
                + "with a heuristic AI that answers instantly. Built with Java Swing and FlatLaf "
                + "as an academic project.", 10));

        card.add(section("HOW TO PLAY"));
        card.add(paragraph("<b>3×3 to 5×5:</b> fill a whole row, column or diagonal.<br>"
                + "<b>6×6 and up:</b> line up 5 pieces in a row in any direction.<br>"
                + "X always moves first. Against the AI you are X.", 0));

        card.add(section("CONTROLS"));
        card.add(paragraph("<b>Mouse wheel</b> zoom &nbsp;·&nbsp; <b>Drag</b> pan the board<br>"
                + "<b>Ctrl+Z</b> undo (when enabled) &nbsp;·&nbsp; <b>R</b> restart &nbsp;·&nbsp; "
                + "<b>Esc</b> back", 0));

        JLabel team = new JLabel("Team “Nhà Chợt Lét”  ·  © 2024  ·  For educational use only");
        team.setFont(Theme.font(Font.PLAIN, 12f));
        team.setForeground(Theme.TEXT_MUTED);
        team.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        left(team);
        card.add(team);

        card.setPreferredSize(new Dimension(CARD_WIDTH, card.getPreferredSize().height));
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);
        add(center, BorderLayout.CENTER);

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
        label.setBorder(BorderFactory.createEmptyBorder(22, 0, 6, 0));
        return left(label);
    }

    private static JComponent paragraph(String html, int gapTop) {
        JLabel label = new JLabel("<html><div style='width:400px;line-height:1.4'>" + html + "</div></html>");
        label.setFont(Theme.font(Font.PLAIN, 14f));
        label.setForeground(Theme.TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(gapTop, 0, 0, 0));
        return left(label);
    }

    private static JComponent pill(String text) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(Theme.alpha(Theme.ACCENT, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(Theme.semibold(12f));
        label.setForeground(Theme.ACCENT_HOVER);
        label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return label;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }
}
