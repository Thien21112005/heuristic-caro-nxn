package view;

import utils.I18n;
import model.Settings;
import utils.SoundManager;
import view.components.AppButton;
import view.components.CenteringScroll;
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
public class SettingsPanel extends PaperPanel {
    private static final int CARD_WIDTH = 480;

    public SettingsPanel(MainWindow window) {
                setLayout(new BorderLayout());
        Runnable back = () -> window.showScreen(MainWindow.MAIN_MENU);
        add(TopBar.withBack(back), BorderLayout.NORTH);

        Settings settings = Settings.get();

        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 28, 36));

        JLabel title = new JLabel(I18n.t("settings.title"));
        title.setFont(Theme.handBold(28f));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);

        card.add(section(I18n.t("settings.sound"), 24));
        card.add(row(I18n.t("settings.music"), I18n.t("settings.music.desc"), settings.isMusicEnabled(), on -> {
            settings.setMusicEnabled(on);
            if (on) SoundManager.startMusic();
            else SoundManager.stopMusic();
        }));
        card.add(volumeRow(settings.getMusicVolume(), v -> {
            settings.setMusicVolume(v);
            SoundManager.setMusicVolume(v);
        }, false));
        card.add(divider());
        card.add(row(I18n.t("settings.sfx"), I18n.t("settings.sfx.desc"), settings.isSfxEnabled(),
                settings::setSfxEnabled));
        card.add(volumeRow(settings.getSfxVolume(), settings::setSfxVolume, true));

        card.add(section(I18n.t("settings.gameplay"), 24));
        card.add(row(I18n.t("settings.undo"), I18n.t("settings.undo.desc"),
                settings.isUndoEnabled(), settings::setUndoEnabled));
        card.add(divider());
        card.add(row(I18n.t("settings.hints"), I18n.t("settings.hints.desc"),
                settings.isHintsEnabled(), settings::setHintsEnabled));

        card.add(section(I18n.t("settings.language"), 24));
        card.add(languagePicker(window));

        JLabel note = new JLabel(I18n.t("settings.saved"));
        note.setFont(Theme.font(Font.PLAIN, 12f));
        note.setForeground(Theme.TEXT_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        card.add(note);

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

    private static JComponent languagePicker(MainWindow window) {
        JPanel picker = new JPanel(new GridLayout(1, 2, 10, 0));
        picker.setOpaque(false);
        picker.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[][] options = { {I18n.ENGLISH, "English"}, {I18n.VIETNAMESE, "Tiếng Việt"} };
        for (String[] option : options) {
            boolean selected = option[0].equals(I18n.getLanguage());
            AppButton b = new AppButton(option[1], selected ? AppButton.Variant.PRIMARY : AppButton.Variant.SECONDARY);
            b.addActionListener(e -> {
                if (option[0].equals(I18n.getLanguage())) return;
                Settings.get().setLanguage(option[0]);
                I18n.setLanguage(option[0]);
                window.rebuildScreens(MainWindow.SETTINGS);
            });
            picker.add(b);
        }
        picker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return picker;
    }

    private static JComponent section(String text, int gapTop) {
        JLabel label = SetupPanel.sectionLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(gapTop, 0, 8, 0));
        return label;
    }

    /**
     * "Volume" slider with a percentage. Changes apply while dragging; with
     * {@code preview} a sample effect plays when the knob is released.
     */
    private static JComponent volumeRow(int value, java.util.function.IntConsumer onChange, boolean preview) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JLabel label = new JLabel(I18n.t("settings.volume"));
        label.setFont(Theme.font(Font.PLAIN, 13f));
        label.setForeground(Theme.TEXT_MUTED);
        JLabel percent = new JLabel(value + "%");
        percent.setFont(Theme.semibold(13f));
        percent.setForeground(Theme.TEXT);
        percent.setPreferredSize(new Dimension(44, 20));
        percent.setHorizontalAlignment(SwingConstants.RIGHT);

        JSlider slider = new JSlider(0, 100, value);
        slider.setOpaque(false);
        slider.setFocusable(true);
        slider.addChangeListener(e -> {
            percent.setText(slider.getValue() + "%");
            onChange.accept(slider.getValue());
            if (preview && !slider.getValueIsAdjusting()) SoundManager.play(SoundManager.CLICK);
        });

        row.add(label, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(percent, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return row;
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
