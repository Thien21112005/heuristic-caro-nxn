package view;

import model.GameConfig;
import model.GameMode;
import model.GameModel;
import model.Settings;
import model.ai.Difficulty;
import utils.I18n;
import view.components.AppButton;
import view.components.BoardView;
import view.components.RoundedPanel;
import view.components.CenteringScroll;
import view.components.SegmentedControl;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Pre-game screen: board size and win rule on the left, mode options
 * (difficulty, ...) on the right. The start button stays on screen however
 * small the window is; the less common choices fold away against the AI.
 */
public class SetupPanel extends PaperPanel {
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 20;
    private static final int[] PRESETS = {3, 10, 15, 20};
    private static final int COLUMN_WIDTH = 380;
    private static final int COLUMN_GAP = 40;
    private static final int PADDING = 32;

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
    private JLabel timeRule;
    private AppButton moreToggle;
    private JPanel moreBody;
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
        buildRoundOptions(right, mode == GameMode.VS_AI ? 16 : 0);
        refresh(); // fill dynamic text before the card measures itself

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 0)) {
            @Override
            public Dimension getPreferredSize() {
                // Fixed width, but the height follows the folding options
                return new Dimension(2 * COLUMN_WIDTH + COLUMN_GAP + 2 * PADDING, super.getPreferredSize().height);
            }
        };
        card.setBorder(BorderFactory.createEmptyBorder(PADDING - 14, PADDING, PADDING - 14, PADDING));

        JPanel header = column();
        JLabel title = new JLabel(I18n.t(mode == GameMode.VS_AI ? "setup.title.vsAi" : "setup.title.twoPlayers"));
        title.setFont(Theme.handBold(Theme.H2));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel(I18n.t(mode == GameMode.VS_AI
                ? "setup.subtitle.vsAi" : "setup.subtitle.twoPlayers"));
        subtitle.setFont(Theme.font(Font.PLAIN, Theme.BODY));
        subtitle.setForeground(Theme.TEXT_MUTED);
        addRow(header, title, 0);
        addRow(header, subtitle, 2);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel body = new JPanel(new GridLayout(1, 2, COLUMN_GAP, 0));
        body.setOpaque(false);
        left.add(Box.createVerticalGlue());
        right.add(Box.createVerticalGlue());
        body.add(left);
        body.add(right);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        add(CenteringScroll.wrapTop(card), BorderLayout.CENTER);

        AppButton start = new AppButton(I18n.t("setup.start"), AppButton.Variant.PRIMARY);
        start.setFont(Theme.hand(Theme.H3));
        start.setPreferredSize(new Dimension(320, 56));
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
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(2, 0, 10, 0));
        footer.add(start);
        add(footer, BorderLayout.SOUTH);

        bindKey(KeyEvent.VK_LEFT, () -> setSize(size - 1));
        bindKey(KeyEvent.VK_RIGHT, () -> setSize(size + 1));
        bindKey(KeyEvent.VK_ENTER, start::doClick);
        bindKey(KeyEvent.VK_ESCAPE, () -> window.showScreen(MainWindow.MAIN_MENU));
    }

    private void buildBoardColumn(JPanel col) {
        sizeLabel.setFont(Theme.handBold(Theme.NUMBER));
        sizeLabel.setForeground(Theme.TEXT);

        minus.compact().setFont(Theme.semibold(Theme.H2));
        plus.compact().setFont(Theme.semibold(Theme.H2));
        minus.setToolTipText(I18n.t("setup.smaller"));
        plus.setToolTipText(I18n.t("setup.bigger"));
        minus.addActionListener(e -> setSize(size - 1));
        plus.addActionListener(e -> setSize(size + 1));

        JPanel stepper = new JPanel(new BorderLayout(12, 0));
        stepper.setOpaque(false);
        minus.setPreferredSize(new Dimension(60, 60));
        plus.setPreferredSize(new Dimension(60, 60));
        stepper.add(minus, BorderLayout.WEST);
        stepper.add(sizeLabel, BorderLayout.CENTER);
        stepper.add(plus, BorderLayout.EAST);

        JPanel presets = new JPanel(new GridLayout(1, PRESETS.length, 4, 0));
        presets.setOpaque(false);
        for (int i = 0; i < PRESETS.length; i++) {
            int preset = PRESETS[i];
            AppButton chip = new AppButton(preset + "×" + preset, AppButton.Variant.PLAIN);
            chip.compact().setFont(Theme.hand(Theme.BODY));
            chip.addActionListener(e -> setSize(preset));
            presetButtons[i] = chip;
            presets.add(chip);
        }

        ruleLabel.setFont(Theme.font(Font.PLAIN, Theme.BODY));
        ruleLabel.setForeground(Theme.TEXT);

        addRow(col, sectionLabel(I18n.t("setup.boardSize")), 0);
        addRow(col, stepper, 8);
        addRow(col, presets, 10);
        addRow(col, ruleLabel, 12);
        stretch(stepper, 62);
        stretch(presets, SegmentedControl.HEIGHT);
    }

    private void buildAiOptions(JPanel col) {
        difficulty = new SegmentedControl<>();
        difficulty.add(Difficulty.EASY, I18n.t("difficulty.easy"))
                .add(Difficulty.NORMAL, I18n.t("difficulty.normal"))
                .add(Difficulty.HARD, I18n.t("difficulty.hard"));
        difficultyHint = new JLabel();
        difficultyHint.setFont(Theme.font(Font.PLAIN, Theme.SMALL));
        difficultyHint.setForeground(Theme.TEXT_MUTED);
        difficulty.setOnChange(d -> refreshDifficultyHint());
        difficulty.setSelected(Settings.get().getDifficulty());

        side = new SegmentedControl<>();
        side.add(GameModel.X, I18n.t("setup.side.x"), new ImageIcon(BoardView.pieceImage(GameModel.X, 28)))
                .add(GameModel.O, I18n.t("setup.side.o"), new ImageIcon(BoardView.pieceImage(GameModel.O, 28)));
        side.setSelected(Settings.get().getHumanSide());

        addRow(col, sectionLabel(I18n.t("setup.difficulty")), 0);
        addRow(col, difficulty, 6);
        addRow(col, difficultyHint, 4);
        addRow(col, sectionLabel(I18n.t("setup.side")), 14);
        addRow(col, side, 6);
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
        timeRule = new JLabel();
        timeRule.setFont(Theme.font(Font.PLAIN, Theme.SMALL));
        timeRule.setForeground(Theme.TEXT_MUTED);
        turnTime.setOnChange(t -> refreshRoundOptions());
        alternate.setOnChange(a -> refreshRoundOptions());

        moreBody = column();
        addRow(moreBody, sectionLabel(I18n.t("setup.firstMove")), 0);
        addRow(moreBody, alternate, 6);
        addRow(moreBody, sectionLabel(I18n.t("setup.turnTime")), 12);
        addRow(moreBody, turnTime, 6);
        addRow(moreBody, timeRule, 4);

        if (mode == GameMode.VS_AI) {
            moreToggle = new AppButton("", AppButton.Variant.GHOST).compact();
            moreToggle.setHorizontalAlignment(SwingConstants.LEFT);
            moreToggle.setFont(Theme.hand(Theme.BODY));
            moreToggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            moreToggle.addActionListener(e -> {
                moreBody.setVisible(!moreBody.isVisible());
                refreshRoundOptions();
                revalidate();
            });
            moreBody.setVisible(false);
            addRow(col, moreToggle, gapBefore - 8);
            addRow(col, moreBody, 6);
        } else {
            addRow(col, moreBody, gapBefore);
        }
        refreshRoundOptions();
    }

    /** The folded row always says what is currently chosen inside it. */
    private void refreshRoundOptions() {
        int seconds = turnTime.getSelected();
        timeRule.setText("<html><div style='width:280px'>" + I18n.t("setup.turnTime.desc") + "</div></html>");
        timeRule.setVisible(seconds != 0);
        if (moreToggle == null) return;
        String time = seconds == 0 ? I18n.t("setup.more.noClock") : I18n.t("setup.turnTime.seconds", seconds);
        String swap = I18n.t(alternate.getSelected() ? "setup.firstMove.alternate" : "setup.firstMove.same");
        moreToggle.setIcon(new ChevronIcon(moreBody.isVisible()));
        moreToggle.setText(I18n.t("setup.more", swap, time));
    }

    private void refreshDifficultyHint() {
        String key = "difficulty." + difficulty.getSelected().name().toLowerCase() + ".desc";
        difficultyHint.setText("<html><div style='width:280px'>" + I18n.t(key) + "</div></html>");
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
            presetButtons[i].setVariant(PRESETS[i] == size ? AppButton.Variant.CIRCLED : AppButton.Variant.PLAIN);
        }
        int target = GameModel.winLengthFor(size);
        ruleLabel.setText("<html><div style='width:280px'><b>" + I18n.t("setup.rule.label") + "</b> " + (target == size
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
        label.setFont(Theme.hand(Theme.BODY));
        label.setForeground(Theme.TEXT);
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

    /** Pen-drawn arrow showing whether the extra options are open. */
    private static final class ChevronIcon implements Icon {
        private final boolean open;

        ChevronIcon(boolean open) {
            this.open = open;
        }

        @Override
        public int getIconWidth() { return 14; }

        @Override
        public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.translate(x, y);
            g2.setColor(c.getForeground());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (open) {
                g2.drawPolyline(new int[]{2, 7, 12}, new int[]{5, 10, 5}, 3);
            } else {
                g2.drawPolyline(new int[]{5, 10, 5}, new int[]{2, 7, 12}, 3);
            }
            g2.dispose();
        }
    }
}
