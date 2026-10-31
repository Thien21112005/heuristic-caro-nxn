package view;

import model.GameConfig;
import model.GameMode;
import model.GameModel;
import model.Settings;
import model.ai.Difficulty;
import utils.I18n;
import view.components.AppButton;
import view.components.RoundedPanel;
import view.components.CenteringScroll;
import view.components.SegmentedControl;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Pre-game screen: board size and win rule on the left, mode options
 * (difficulty, ...) on the right, then start.
 */
public class SetupPanel extends PaperPanel {
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 20;
    private static final int[] PRESETS = {3, 10, 15, 20};
    private static final int COLUMN_WIDTH = 400;
    private static final int COLUMN_GAP = 40;
    private static final int PADDING = 40;

    private final GameMode mode;
    private int size;

    private final JLabel sizeLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel ruleLabel = new JLabel();
    private final AppButton minus = new AppButton("−", AppButton.Variant.SECONDARY);
    private final AppButton plus = new AppButton("+", AppButton.Variant.SECONDARY);
    private final AppButton[] presetButtons = new AppButton[PRESETS.length];
    private SegmentedControl<Difficulty> difficulty;
    private JLabel difficultyHint;
    private SegmentedControl<Integer> side;
    private SegmentedControl<Boolean> alternate;
    private SegmentedControl<Integer> turnTime;
    private static final int[] TURN_TIMES = {0, 10, 20, 30, 60};

    public SetupPanel(MainWindow window, GameMode mode) {
                this.mode = mode;
        this.size = clamp(Settings.get().getBoardSize(mode));
        setLayout(new BorderLayout());

        add(TopBar.withBack(() -> window.showScreen(MainWindow.MAIN_MENU)), BorderLayout.NORTH);

        JPanel left = column();
        buildBoardColumn(left);
        JPanel right = column();
        if (mode == GameMode.VS_AI) buildAiOptions(right);
        buildRoundOptions(right, mode == GameMode.VS_AI ? 20 : 0);
        refresh(); // fill dynamic text before the card measures itself

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createEmptyBorder(PADDING - 8, PADDING, PADDING - 8, PADDING));

        JPanel header = column();
        JLabel title = new JLabel(I18n.t(mode == GameMode.VS_AI ? "setup.title.vsAi" : "setup.title.twoPlayers"));
        title.setFont(Theme.handBold(32f));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel(I18n.t(mode == GameMode.VS_AI
                ? "setup.subtitle.vsAi" : "setup.subtitle.twoPlayers"));
        subtitle.setFont(Theme.font(Font.PLAIN, 17f));
        subtitle.setForeground(Theme.TEXT_MUTED);
        addRow(header, title, 0);
        addRow(header, subtitle, 8);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel body = new JPanel(new GridLayout(1, 2, COLUMN_GAP, 0));
        body.setOpaque(false);
        left.add(Box.createVerticalGlue());
        right.add(Box.createVerticalGlue());
        body.add(left);
        body.add(right);

        AppButton start = new AppButton(I18n.t("setup.start"), AppButton.Variant.PRIMARY);
        start.setFont(Theme.semibold(20f));
        start.setPreferredSize(new Dimension(10, 62));
        start.addActionListener(e -> {
            Settings.get().setBoardSize(mode, size);
            boolean swap = alternate.getSelected();
            int seconds = turnTime.getSelected();
            Settings.get().setAlternateFirst(mode, swap);
            Settings.get().setTurnSeconds(mode, seconds);
            if (mode == GameMode.VS_AI) {
                Settings.get().setDifficulty(difficulty.getSelected());
                Settings.get().setHumanSide(side.getSelected());
                window.startGame(GameConfig.vsAi(size, difficulty.getSelected(), side.getSelected(), swap)
                        .withTurnSeconds(seconds));
            } else {
                window.startGame(GameConfig.twoPlayers(size, swap).withTurnSeconds(seconds));
            }
        });
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        footer.add(start);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        int width = 2 * COLUMN_WIDTH + COLUMN_GAP + 2 * PADDING;
        card.setPreferredSize(new Dimension(width, card.getPreferredSize().height));

        add(CenteringScroll.wrap(card), BorderLayout.CENTER);

        bindKey(KeyEvent.VK_LEFT, () -> setSize(size - 1));
        bindKey(KeyEvent.VK_RIGHT, () -> setSize(size + 1));
        bindKey(KeyEvent.VK_ENTER, start::doClick);
        bindKey(KeyEvent.VK_ESCAPE, () -> window.showScreen(MainWindow.MAIN_MENU));
    }

    private void buildBoardColumn(JPanel col) {
        sizeLabel.setFont(Theme.handBold(54f));
        sizeLabel.setForeground(Theme.TEXT);

        minus.compact().setFont(Theme.semibold(28f));
        plus.compact().setFont(Theme.semibold(28f));
        minus.setToolTipText(I18n.t("setup.smaller"));
        plus.setToolTipText(I18n.t("setup.bigger"));
        minus.addActionListener(e -> setSize(size - 1));
        plus.addActionListener(e -> setSize(size + 1));

        JPanel stepper = new JPanel(new BorderLayout(12, 0));
        stepper.setOpaque(false);
        minus.setPreferredSize(new Dimension(68, 68));
        plus.setPreferredSize(new Dimension(68, 68));
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

        RoundedPanel rule = new RoundedPanel(Theme.RADIUS) {
            @Override
            public Dimension getMaximumSize() {
                // Grow with the text (Vietnamese wraps to two lines) but never stretch further
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        rule.setFill(Theme.alpha(Theme.ACCENT, 28));
        rule.setBorderColor(Theme.alpha(Theme.ACCENT, 90));
        rule.setLayout(new BorderLayout());
        rule.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        ruleLabel.setFont(Theme.font(Font.PLAIN, 15f));
        ruleLabel.setForeground(Theme.TEXT);
        rule.add(ruleLabel);

        addRow(col, sectionLabel(I18n.t("setup.boardSize")), 0);
        addRow(col, stepper, 12);
        addRow(col, presets, 14);
        addRow(col, rule, 18);
        stretch(stepper, 72);
        stretch(presets, 46);
    }

    private void buildAiOptions(JPanel col) {
        difficulty = new SegmentedControl<>();
        difficulty.add(Difficulty.EASY, I18n.t("difficulty.easy"))
                .add(Difficulty.NORMAL, I18n.t("difficulty.normal"))
                .add(Difficulty.HARD, I18n.t("difficulty.hard"));
        difficultyHint = new JLabel();
        difficultyHint.setFont(Theme.font(Font.PLAIN, 14f));
        difficultyHint.setForeground(Theme.TEXT_MUTED);
        difficulty.setOnChange(d -> refreshDifficultyHint());
        difficulty.setSelected(Settings.get().getDifficulty());

        side = new SegmentedControl<>();
        side.add(GameModel.X, I18n.t("setup.side.x")).add(GameModel.O, I18n.t("setup.side.o"));
        side.setSelected(Settings.get().getHumanSide());

        addRow(col, sectionLabel(I18n.t("setup.difficulty")), 0);
        addRow(col, difficulty, 12);
        addRow(col, difficultyHint, 10);
        addRow(col, sectionLabel(I18n.t("setup.side")), 20);
        addRow(col, side, 12);
        refreshDifficultyHint();
    }

    private void buildRoundOptions(JPanel col, int gapBefore) {
        alternate = new SegmentedControl<>();
        alternate.add(false, I18n.t("setup.firstMove.same")).add(true, I18n.t("setup.firstMove.alternate"));
        alternate.setSelected(Settings.get().isAlternateFirst(mode));
        alternate.setToolTipText(I18n.t("setup.firstMove.desc"));

        turnTime = new SegmentedControl<>();
        for (int seconds : TURN_TIMES) {
            turnTime.add(seconds, seconds == 0 ? I18n.t("setup.turnTime.off") : I18n.t("setup.turnTime.seconds", seconds));
        }
        int saved = Settings.get().getTurnSeconds(mode);
        turnTime.setSelected(java.util.Arrays.stream(TURN_TIMES).anyMatch(t -> t == saved) ? saved : 0);
        turnTime.setToolTipText(I18n.t("setup.turnTime.desc"));

        addRow(col, sectionLabel(I18n.t("setup.firstMove")), gapBefore);
        addRow(col, alternate, 12);
        addRow(col, sectionLabel(I18n.t("setup.turnTime")), 20);
        addRow(col, turnTime, 12);
    }

    private void refreshDifficultyHint() {
        String key = "difficulty." + difficulty.getSelected().name().toLowerCase() + ".desc";
        difficultyHint.setText("<html><div style='width:300px'>" + I18n.t(key) + "</div></html>");
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
        ruleLabel.setText("<html><div style='width:270px'><b>" + I18n.t("setup.rule.label") + "</b> " + (target == size
                ? I18n.t("setup.rule.fullLine")
                : I18n.t("setup.rule.inARow", target)) + "</div></html>");
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

    private static JPanel column() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        return col;
    }

    private static void addRow(JPanel col, JComponent c, int gapBefore) {
        if (gapBefore > 0) col.add(Box.createVerticalStrut(gapBefore));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(c);
    }

    private static void stretch(JComponent c, int height) {
        c.setPreferredSize(new Dimension(COLUMN_WIDTH, height));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }
}
