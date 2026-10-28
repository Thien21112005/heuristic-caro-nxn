package view;

import model.GameMode;
import model.GameModel;
import model.Settings;
import view.components.AppButton;
import view.components.RoundedPanel;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Pre-game screen: pick the board size, see the win rule, start.
 */
public class SetupPanel extends BackgroundPanel {
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 20;
    private static final int[] PRESETS = {3, 10, 15, 20};
    private static final int CARD_WIDTH = 440;

    private final GameMode mode;
    private int size;

    private final JLabel sizeLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel ruleLabel = new JLabel();
    private final AppButton minus = new AppButton("−", AppButton.Variant.SECONDARY);
    private final AppButton plus = new AppButton("+", AppButton.Variant.SECONDARY);
    private final AppButton[] presetButtons = new AppButton[PRESETS.length];

    public SetupPanel(MainWindow window, GameMode mode) {
        super("/assets/Background.jpg");
        this.mode = mode;
        this.size = clamp(Settings.get().getBoardSize(mode));
        setLayout(new BorderLayout());

        add(TopBar.withBack(() -> window.showScreen(MainWindow.MAIN_MENU)), BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        JLabel title = new JLabel(mode == GameMode.VS_AI ? "Play vs AI" : "Two players");
        title.setFont(Theme.font(Font.BOLD, 28f));
        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel(mode == GameMode.VS_AI
                ? "You play X and move first. The AI plays O."
                : "Player 1 is X and moves first, Player 2 is O.");
        subtitle.setFont(Theme.font(Font.PLAIN, 14f));
        subtitle.setForeground(Theme.TEXT_MUTED);

        sizeLabel.setFont(Theme.font(Font.BOLD, 40f));
        sizeLabel.setForeground(Theme.TEXT);

        minus.compact().setFont(Theme.semibold(22f));
        plus.compact().setFont(Theme.semibold(22f));
        minus.setToolTipText("Smaller board (←)");
        plus.setToolTipText("Bigger board (→)");
        minus.addActionListener(e -> setSize(size - 1));
        plus.addActionListener(e -> setSize(size + 1));

        JPanel stepper = new JPanel(new BorderLayout(12, 0));
        stepper.setOpaque(false);
        square(minus, 52);
        square(plus, 52);
        stepper.add(minus, BorderLayout.WEST);
        stepper.add(sizeLabel, BorderLayout.CENTER);
        stepper.add(plus, BorderLayout.EAST);

        JPanel presets = new JPanel(new GridLayout(1, PRESETS.length, 8, 0));
        presets.setOpaque(false);
        for (int i = 0; i < PRESETS.length; i++) {
            int preset = PRESETS[i];
            AppButton chip = new AppButton(preset + "×" + preset, AppButton.Variant.SECONDARY);
            chip.compact().setFont(Theme.semibold(13f));
            chip.addActionListener(e -> setSize(preset));
            presetButtons[i] = chip;
            presets.add(chip);
        }

        RoundedPanel rule = new RoundedPanel(Theme.RADIUS);
        rule.setFill(Theme.alpha(Theme.ACCENT, 28));
        rule.setBorderColor(Theme.alpha(Theme.ACCENT, 90));
        rule.setLayout(new BorderLayout());
        rule.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        ruleLabel.setFont(Theme.font(Font.PLAIN, 14f));
        ruleLabel.setForeground(Theme.TEXT);
        rule.add(ruleLabel);

        AppButton start = new AppButton("Start game", AppButton.Variant.PRIMARY);
        start.setFont(Theme.semibold(17f));
        start.addActionListener(e -> {
            Settings.get().setBoardSize(mode, size);
            window.startGame(mode, size);
        });

        addRow(card, title, 0);
        addRow(card, subtitle, 6);
        addRow(card, sectionLabel("BOARD SIZE"), 28);
        addRow(card, stepper, 10);
        addRow(card, presets, 14);
        addRow(card, rule, 20);
        addRow(card, start, 28);
        stretch(stepper, 56);
        stretch(presets, 38);
        stretch(rule, 64);
        stretch(start, 54);

        Dimension cardSize = new Dimension(CARD_WIDTH, card.getPreferredSize().height);
        card.setPreferredSize(cardSize);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);
        add(center, BorderLayout.CENTER);

        bindKey(KeyEvent.VK_LEFT, () -> setSize(size - 1));
        bindKey(KeyEvent.VK_RIGHT, () -> setSize(size + 1));
        bindKey(KeyEvent.VK_ENTER, start::doClick);
        bindKey(KeyEvent.VK_ESCAPE, () -> window.showScreen(MainWindow.MAIN_MENU));

        refresh();
    }

    private void setSize(int newSize) {
        size = clamp(newSize);
        refresh();
    }

    private void refresh() {
        sizeLabel.setText(size + " × " + size);
        minus.setEnabled(size > MIN_SIZE);
        plus.setEnabled(size < MAX_SIZE);
        for (int i = 0; i < PRESETS.length; i++) {
            presetButtons[i].setVariant(PRESETS[i] == size ? AppButton.Variant.PRIMARY : AppButton.Variant.SECONDARY);
        }
        int target = GameModel.winLengthFor(size);
        ruleLabel.setText("<html><b>Win rule:</b> " + (target == size
                ? "fill a whole row, column or diagonal (" + target + " in a row)."
                : "line up " + target + " pieces in a row — any direction.") + "</html>");
    }

    private static int clamp(int value) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, value));
    }

    private void bindKey(int key, Runnable action) {
        String id = "key" + key;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), id);
        getActionMap().put(id, new AbstractAction() {
            @Override
            public boolean isEnabled() {
                // Every screen lives in the same window; only the visible one may react
                return isShowing();
            }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.semibold(12f));
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    private static void addRow(JPanel card, JComponent c, int gapBefore) {
        if (gapBefore > 0) card.add(Box.createVerticalStrut(gapBefore));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(c);
    }

    private static void stretch(JComponent c, int height) {
        c.setPreferredSize(new Dimension(CARD_WIDTH - 72, height));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    private static void square(JComponent c, int side) {
        c.setPreferredSize(new Dimension(side, side));
    }
}
