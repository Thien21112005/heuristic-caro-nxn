package view;

import model.Settings;
import utils.SoundManager;
import view.components.RoundedPanel;
import view.components.ToggleSwitch;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Sound and gameplay preferences. Changes apply and save immediately.
 */
public class SettingsPanel extends BackgroundPanel {
    private static final int CARD_WIDTH = 480;

    public SettingsPanel(GameMenu window) {
        super("/assets/Background.jpg");
        setLayout(new BorderLayout());
        Runnable back = () -> window.showScreen(GameMenu.MAIN_MENU);
        add(TopBar.withBack(back), BorderLayout.NORTH);

        Settings settings = Settings.get();

        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 28, 36));

        JLabel title = new JLabel("Settings");
        title.setFont(Theme.font(Font.BOLD, 28f));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);

        card.add(section("SOUND", 24));
        card.add(row("Background music", "Loop the soundtrack while you play", settings.isMusicEnabled(), on -> {
            settings.setMusicEnabled(on);
            if (on) SoundManager.startMusic();
            else SoundManager.stopMusic();
        }));
        card.add(divider());
        card.add(row("Sound effects", "Clicks, hovers and the victory jingle", settings.isSfxEnabled(),
                settings::setSfxEnabled));

        card.add(section("GAMEPLAY", 24));
        card.add(row("Allow undo", "Take back moves (Ctrl+Z). Vs AI it also undoes the AI's reply",
                settings.isUndoEnabled(), settings::setUndoEnabled));

        JLabel note = new JLabel("Changes are saved automatically.");
        note.setFont(Theme.font(Font.PLAIN, 12f));
        note.setForeground(Theme.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        card.add(note);

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

    private static JComponent section(String text, int gapTop) {
        JLabel label = SetupPanel.sectionLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(gapTop, 0, 8, 0));
        return label;
    }

    private static JComponent divider() {
        JPanel line = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.alpha(Theme.BORDER, 160));
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        };
        line.setOpaque(false);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9));
        line.setPreferredSize(new Dimension(10, 9));
        return line;
    }

    private static JComponent row(String title, String description, boolean value, Consumer<Boolean> onChange) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel("<html><div style='width:250px'><span style='font-size:13pt'>" + title
                + "</span><br><span style='color:#969cc4'>" + description + "</span></div></html>");
        label.setFont(Theme.font(Font.PLAIN, 13f));
        label.setForeground(Theme.TEXT);

        ToggleSwitch toggle = new ToggleSwitch(value);
        toggle.setOnChange(onChange);
        // The whole row is a click target, not just the small switch
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle.toggle();
            }
        });

        row.add(label, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
