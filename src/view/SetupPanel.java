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
    private static final int CARD_WIDTH = 560;

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
        card.setBorder(BorderFactory.createEmptyBorder(40, 44, 40, 44));

        JLabel title = new JLabel(mode == GameMode.VS_AI ? "Play vs AI" : "Two players");
        title.setFont(Theme.font(Font.BOLD, 36f));
        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel(mode == GameMode.VS_AI
                ? "You play X and move first. The AI plays O."
                : "Player 1 is X and moves first, Player 2 is O.");
        subtitle.setFont(Theme.font(Font.PLAIN, 17f));
        subtitle.setForeground(Theme.TEXT_MUTED);

        sizeLabel.setFont(Theme.font(Font.BOLD, 54f));
        sizeLabel.setForeground(Theme.TEXT);

        minus.compact().setFont(Theme.semibold(28f));
        plus.compact().setFont(Theme.semibold(28f));
        minus.setToolTipText("Smaller board (←)");
        plus.setToolTipText("Bigger board (→)");
        minus.addActionListener(e -> setSize(size - 1));
        plus.addActionListener(e -> setSize(size + 1));

        JPanel stepper = new JPanel(new BorderLayout(12, 0));
        stepper.setOpaque(false);
        square(minus, 68);
        square(plus, 68);
        stepper.add(minus, BorderLayout.WEST);
        stepper.add(sizeLabel, BorderLayout.CENTER);
        stepper.add(plus, BorderLayout.EAST);

        JPanel presets = new JPanel(new GridLayout(1, PRESETS.length, 8, 0));
        presets.setOpaque(false);
        for (int i = 0; i < PRESETS.length; i++) {
            int preset = PRESETS[i];
            AppButton chip = new AppButton(preset + "×" + preset, AppButton.Variant.SECONDARY);
            chip.compact().setFont(Theme.semibold(16f));
            chip.addActionListener(e -> setSize(preset));
            presetButtons[i] = chip;
            presets.add(chip);
        }

        RoundedPanel rule = new RoundedPanel(Theme.RADIUS);
        rule.setFill(Theme.alpha(Theme.ACCENT, 28));
        rule.setBorderColor(Theme.alpha(Theme.ACCENT, 90));
        rule.setLayout(new BorderLayout());
        rule.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        ruleLabel.setFont(Theme.font(Font.PLAIN, 16f));
        ruleLabel.setForeground(Theme.TEXT);
        rule.add(ruleLabel);

        AppButton start = new AppButton("Start game", AppButton.Variant.PRIMARY);
        start.setFont(Theme.semibold(20f));
        start.addActionListener(e -> {
            Settings.get().setBoardSize(mode, size);
            window.startGame(mode, size);
        });

        addRow(card, title, 0);
        addRow(card, subtitle, 8);
        addRow(card, sectionLabel("BOARD SIZE"), 34);
        addRow(card, stepper, 12);
        addRow(card, presets, 16);
        addRow(card, rule, 24);
        addRow(card, start, 34);
        stretch(stepper, 72);
        stretch(presets, 48);
        stretch(rule, 76);
        stretch(start, 64);

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
                ? "fill a whole row, column or diagonal."
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
        label.setFont(Theme.semibold(13f));
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    private static void addRow(JPanel card, JComponent c, int gapBefore) {
        if (gapBefore > 0) card.add(Box.createVerticalStrut(gapBefore));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(c);
    }

    private static void stretch(JComponent c, int height) {
        c.setPreferredSize(new Dimension(CARD_WIDTH - 88, height));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    private static void square(JComponent c, int side) {
        c.setPreferredSize(new Dimension(side, side));
    }
}
