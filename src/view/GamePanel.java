package view;

import utils.I18n;
import controller.GameController;
import model.GameConfig;
import model.GameMode;
import model.GameModel;
import model.Settings;
import utils.SoundManager;
import view.components.AppButton;
import view.components.BoardView;
import view.components.PlayerCard;
import view.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * The in-game screen: header with actions, player cards either side of the
 * board, and a status bar with zoom controls.
 */
public class GamePanel extends BackgroundPanel implements GameController.Listener {
    private static final int GAME_OVER_DELAY_MS = 900;

    private final MainWindow window;
    private final GameController controller;
    private final GameModel model;
    private final GameMode mode;
    private final GameConfig config;

    private final BoardView board;
    private final JScrollPane scroll;
    // Seat 0 is "You" / Player 1 (left card), seat 1 is the AI / Player 2 (right card).
    // Seats keep their score even if their pieces change between rounds.
    private final PlayerCard[] cards = new PlayerCard[2];
    private final JLabel status = new JLabel("", SwingConstants.CENTER);
    private final AppButton undo;

    private final int[] scores = new int[2];
    private int draws;
    private int lastMoveCount;
    private boolean autoFit = true;
    private Timer gameOverTimer;

    public GamePanel(MainWindow window, GameConfig config) {
        super("/assets/Background.jpg");
        setDim(40, 150);
        this.window = window;
        this.config = config;
        this.mode = config.mode();
        int size = config.size();
        this.controller = new GameController(config);
        this.model = controller.getModel();
        setLayout(new BorderLayout());

        cards[0] = new PlayerCard(sideOf(0), seatName(0));
        cards[1] = new PlayerCard(sideOf(1), seatName(1));
        String level = mode == GameMode.VS_AI ? difficultyLabel(config.difficulty()) : null;
        cards[1].setTag(level);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 6, 20));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(level == null ? mode.getLabel() : mode.getLabel() + "  ·  " + level);
        title.setFont(Theme.font(Font.BOLD, 20f));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        int target = model.getTarget();
        JLabel subtitle = new JLabel(I18n.t("game.subtitle", size, target));
        subtitle.setFont(Theme.font(Font.PLAIN, 13f));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        titles.add(title);
        titles.add(subtitle);

        AppButton menu = TopBar.backButton(I18n.t("common.back"), this::requestLeave);
        undo = new AppButton(I18n.t("game.undo"), AppButton.Variant.SECONDARY);
        undo.setToolTipText(I18n.t("game.undo.tooltip"));
        undo.setVisible(Settings.get().isUndoEnabled());
        undo.addActionListener(e -> undoMove());
        AppButton restart = new AppButton(I18n.t("game.restart"), AppButton.Variant.SECONDARY);
        restart.setToolTipText(I18n.t("game.restart.tooltip"));
        restart.addActionListener(e -> requestRestart());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(undo);
        actions.add(restart);

        // Keep the title centered by giving both sides the same width
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(menu);
        Dimension side = new Dimension(260, actions.getPreferredSize().height);
        left.setPreferredSize(side);
        actions.setPreferredSize(side);

        header.add(left, BorderLayout.WEST);
        header.add(titles, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Board
        board = new BoardView(controller);
        board.setOnZoom(() -> autoFit = false);
        scroll = new JScrollPane(new BoardView.Centered(board));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setWheelScrollingEnabled(false);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (autoFit) fitBoard();
            }
        });

        JPanel center = new JPanel(new BorderLayout(20, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(6, 24, 0, 24));
        center.add(wrapCard(cards[0]), BorderLayout.WEST);
        center.add(scroll, BorderLayout.CENTER);
        center.add(wrapCard(cards[1]), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // Status bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 24, 14, 24));
        status.setFont(Theme.semibold(17f));
        status.setForeground(Theme.TEXT);

        JLabel hint = new JLabel(I18n.t("game.hint.controls"));
        hint.setFont(Theme.font(Font.PLAIN, 12f));
        hint.setForeground(Theme.TEXT_MUTED);

        AppButton zoomOut = zoomButton("−", I18n.t("game.zoomOut.tooltip"), () -> zoomBy(-1));
        AppButton fit = zoomButton(I18n.t("game.fit"), I18n.t("game.fit.tooltip"), () -> {
            autoFit = true;
            fitBoard();
        });
        AppButton zoomIn = zoomButton("+", I18n.t("game.zoomIn.tooltip"), () -> zoomBy(1));
        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoom.setOpaque(false);
        zoom.add(zoomOut);
        zoom.add(fit);
        zoom.add(zoomIn);

        JPanel hintBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        hintBox.setOpaque(false);
        hintBox.add(hint);
        Dimension footerSide = new Dimension(Math.max(240, zoom.getPreferredSize().width), zoom.getPreferredSize().height);
        hintBox.setPreferredSize(footerSide);
        zoom.setPreferredSize(footerSide);

        footer.add(hintBox, BorderLayout.WEST);
        footer.add(status, BorderLayout.CENTER);
        footer.add(zoom, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), () -> {
            if (undo.isVisible()) undoMove();
        });
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), this::requestRestart);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this::requestLeave);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), fit::doClick);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), () -> zoomBy(1));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), () -> zoomBy(1));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), () -> zoomBy(-1));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), () -> zoomBy(-1));

        controller.setListener(this);
        onStateChanged();
        controller.start();
    }

    /** Stops timers; call when the screen is discarded. */
    public void dispose() {
        if (gameOverTimer != null) gameOverTimer.stop();
        controller.dispose();
    }

    // ---- GameController.Listener ----

    @Override
    public void onStateChanged() {
        if (model.getMoveCount() > lastMoveCount) SoundManager.play(SoundManager.CLICK);
        lastMoveCount = model.getMoveCount();

        undo.setEnabled(controller.canUndo());
        board.stateChanged();
        refreshStatus();
    }

    @Override
    public void onGameOver() {
        int winner = model.getWinner();
        if (winner == GameModel.EMPTY) draws++;
        else scores[seatOf(winner)]++;
        refreshScores();
        refreshStatus();

        boolean humanLost = mode == GameMode.VS_AI && winner == controller.getAiSide();
        SoundManager.play(winner == GameModel.EMPTY || humanLost ? SoundManager.ALERT : SoundManager.WIN);

        // Give the player a moment to see the winning line before the dialog covers it
        gameOverTimer = new Timer(GAME_OVER_DELAY_MS, e -> showGameOverDialog());
        gameOverTimer.setRepeats(false);
        gameOverTimer.start();
    }

    @Override
    public void onAiThinking(boolean thinking) {
        refreshStatus();
    }

    // ---- Actions ----

    private void showGameOverDialog() {
        gameOverTimer = null;
        if (!isShowing() || !model.isOver()) return;
        int option = CustomDialog.confirm(window, resultTitle(), scoreLine(), I18n.t("game.over.playAgain"),
                I18n.t("game.over.viewBoard"), false);
        if (option == CustomDialog.YES_OPTION) controller.restart();
    }

    private void undoMove() {
        // Taking back the winning move also takes back the point it scored
        if (model.isOver()) {
            int winner = model.getWinner();
            if (winner == GameModel.EMPTY) draws--;
            else scores[seatOf(winner)]--;
            refreshScores();
        }
        cancelGameOverDialog();
        controller.undo();
    }

    private void requestRestart() {
        if (inProgress()) {
            int option = CustomDialog.confirm(window, I18n.t("game.restart.title"),
                    I18n.t("game.restart.message"), I18n.t("game.restart.confirm"), I18n.t("common.cancel"), false);
            if (option != CustomDialog.YES_OPTION) return;
        }
        cancelGameOverDialog();
        controller.restart();
    }

    private void requestLeave() {
        if (inProgress()) {
            int option = CustomDialog.confirm(window, I18n.t("game.leave.title"),
                    I18n.t("game.leave.message"), I18n.t("game.leave.confirm"), I18n.t("game.leave.stay"), true);
            if (option != CustomDialog.YES_OPTION) return;
        }
        dispose();
        window.showScreen(mode == GameMode.VS_AI ? MainWindow.SETUP_AI : MainWindow.SETUP_TWO);
    }

    private boolean inProgress() {
        return model.getMoveCount() > 0 && !model.isOver();
    }

    private void cancelGameOverDialog() {
        if (gameOverTimer != null) {
            gameOverTimer.stop();
            gameOverTimer = null;
        }
    }

    private void fitBoard() {
        Dimension area = scroll.getViewport().getExtentSize();
        if (area.width <= 0 || area.height <= 0) return;
        board.setCellSize(BoardView.fitCellSize(model.getSize(), area));
    }

    private void zoomBy(int direction) {
        int step = Math.max(4, board.getCellSize() / 6);
        Point center = new Point(board.getWidth() / 2, board.getHeight() / 2);
        Rectangle visible = board.getVisibleRect();
        if (!visible.isEmpty()) center = new Point((int) visible.getCenterX(), (int) visible.getCenterY());
        board.zoomAt(board.getCellSize() + direction * step, center);
    }

    // ---- Presentation ----

    private void refreshStatus() {
        int current = model.getCurrentPlayer();
        int winner = model.getWinner();
        boolean thinking = controller.isAiThinking();

        for (int seat = 0; seat < 2; seat++) {
            cards[seat].setPiece(sideOf(seat));
            boolean seatThinking = thinking && mode == GameMode.VS_AI && seat == 1;
            cards[seat].setStatus(statusFor(sideOf(seat), current, winner, seatThinking));
        }

        String text;
        Color color = Theme.TEXT;
        if (winner != GameModel.EMPTY) {
            text = resultTitle();
            color = Theme.WIN;
        } else if (model.isDraw()) {
            text = I18n.t("game.status.draw");
        } else if (thinking) {
            text = I18n.t("game.status.aiThinking");
            color = controller.getAiSide() == GameModel.X ? Theme.X_COLOR : Theme.O_COLOR;
        } else if (mode == GameMode.VS_AI) {
            text = I18n.t("game.status.yourTurn", controller.getHumanSide() == GameModel.X ? "X" : "O");
            color = controller.getHumanSide() == GameModel.X ? Theme.X_COLOR : Theme.O_COLOR;
        } else {
            boolean x = current == GameModel.X;
            text = I18n.t("game.status.turn", nameOf(current), x ? "X" : "O");
            color = x ? Theme.X_COLOR : Theme.O_COLOR;
        }
        status.setText(text);
        status.setForeground(color);
    }

    private static PlayerCard.Status statusFor(int player, int current, int winner, boolean thinking) {
        if (winner != GameModel.EMPTY) return winner == player ? PlayerCard.Status.WINNER : PlayerCard.Status.WAITING;
        if (current != player) return PlayerCard.Status.WAITING;
        return thinking ? PlayerCard.Status.THINKING : PlayerCard.Status.TURN;
    }

    private String resultTitle() {
        int winner = model.getWinner();
        if (winner == GameModel.EMPTY) return I18n.t("game.status.draw");
        if (mode == GameMode.VS_AI) {
            return I18n.t(winner == controller.getHumanSide() ? "game.result.youWin" : "game.result.aiWins");
        }
        return I18n.t("game.result.wins", nameOf(winner));
    }

    private void refreshScores() {
        cards[0].setScore(scores[0]);
        cards[1].setScore(scores[1]);
    }

    private String scoreLine() {
        String a = seatName(0);
        String b = seatName(1);
        // The dialog renders HTML, so plain double spaces would collapse
        String line = a + "&nbsp;&nbsp;<b style='color:#ECEEFA'>" + scores[0] + " – " + scores[1] + "</b>&nbsp;&nbsp;" + b;
        if (draws > 0) line += "\n" + I18n.t("game.result.draws", draws);
        return line;
    }

    private static String difficultyLabel(model.ai.Difficulty difficulty) {
        return I18n.t("difficulty." + difficulty.name().toLowerCase());
    }

    /** Piece played by a seat this round; seat 0 always holds the controller's human side. */
    private int sideOf(int seat) {
        return seat == 0 ? controller.getHumanSide() : GameModel.opponent(controller.getHumanSide());
    }

    private int seatOf(int piece) {
        return piece == controller.getHumanSide() ? 0 : 1;
    }

    private String seatName(int seat) {
        if (mode == GameMode.VS_AI) return I18n.t(seat == 0 ? "player.you" : "player.ai");
        return I18n.t(seat == 0 ? "player.one" : "player.two");
    }

    private String nameOf(int piece) {
        return seatName(seatOf(piece));
    }

    private static JComponent wrapCard(PlayerCard card) {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(card);
        return holder;
    }

    private static AppButton zoomButton(String text, String tooltip, Runnable action) {
        AppButton b = new AppButton(text, AppButton.Variant.SECONDARY).compact();
        b.setToolTipText(tooltip);
        b.setFont(Theme.semibold(text.length() > 1 ? 13f : 18f));
        int width = text.length() > 1 ? Math.max(52, b.getPreferredSize().width) : 40;
        b.setPreferredSize(new Dimension(width, 40));
        b.addActionListener(e -> action.run());
        return b;
    }

    private void bindKey(KeyStroke stroke, Runnable action) {
        String id = stroke.toString();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, id);
        getActionMap().put(id, new AbstractAction() {
            @Override
            public boolean isEnabled() { return isShowing(); }

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
        });
    }
}
