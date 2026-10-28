package view;

import utils.I18n;
import controller.GameController;
import model.EndReason;
import model.GameConfig;
import model.GameMode;
import model.GameModel;
import model.Settings;
import model.Stats;
import utils.SoundManager;
import view.components.AppButton;
import view.components.BoardView;
import view.components.PlayerCard;
import view.components.ReplayBar;
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
    private final AppButton hint;
    private final AppButton resign;
    private final AppButton numbers;

    // Replay of a finished game
    private static final int REPLAY_STEP_MS = 600;
    private final ReplayBar replayBar = new ReplayBar();
    private final Timer replayTimer;
    private java.util.List<int[]> replayMoves;
    private int replayIndex;

    private final int[] scores = new int[2];
    private int draws;
    private int hintsUsed;
    private long lastTickSecond = -1;
    private Stats.Entry statsBeforeLastGame; // lets an undone winning move un-record the result
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
        hint = new AppButton(I18n.t("game.hint"), AppButton.Variant.SECONDARY);
        hint.setToolTipText(I18n.t("game.hint.tooltip"));
        hint.addActionListener(e -> showHint());
        hint.setVisible(Settings.get().isHintsEnabled());
        resign = new AppButton(I18n.t("game.resign"), AppButton.Variant.GHOST).setAccent(Theme.DANGER);
        resign.setToolTipText(I18n.t("game.resign.tooltip"));
        resign.addActionListener(e -> requestResign());
        AppButton restart = new AppButton(I18n.t("game.restart"), AppButton.Variant.SECONDARY);
        restart.setToolTipText(I18n.t("game.restart.tooltip"));
        restart.addActionListener(e -> requestRestart());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(resign);
        actions.add(hint);
        actions.add(undo);
        actions.add(restart);

        // Keep the title centered by giving both sides the same width
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(menu);
        Dimension side = new Dimension(Math.max(260, actions.getPreferredSize().width), actions.getPreferredSize().height);
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

        JPanel boardColumn = new JPanel(new BorderLayout(0, 10));
        boardColumn.setOpaque(false);
        boardColumn.add(scroll, BorderLayout.CENTER);
        boardColumn.add(replayBar, BorderLayout.SOUTH);
        replayBar.setVisible(false);
        replayBar.setOnSeek(this::seekReplay);
        replayBar.setOnTogglePlay(this::toggleReplayPlayback);
        replayTimer = new Timer(REPLAY_STEP_MS, e -> {
            if (replayIndex >= replayMoves.size()) stopReplayPlayback();
            else seekReplay(replayIndex + 1);
        });

        JPanel center = new JPanel(new BorderLayout(20, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(6, 24, 0, 24));
        center.add(wrapCard(cards[0]), BorderLayout.WEST);
        center.add(boardColumn, BorderLayout.CENTER);
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
        numbers = zoomButton("#", I18n.t("game.numbers.tooltip"), this::toggleMoveNumbers);
        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoom.setOpaque(false);
        zoom.add(numbers);
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
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), () -> {
            if (hint.isVisible()) showHint();
        });
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), this::requestRestart);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this::requestLeave);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), fit::doClick);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), this::toggleMoveNumbers);
        // Replay controls; they only act while the replay bar is showing
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), () -> replayStep(() -> replayIndex - 1));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), () -> replayStep(() -> replayIndex + 1));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), () -> replayStep(() -> 0));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), () -> replayStep(() -> Integer.MAX_VALUE));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), () -> {
            if (ensureReplay()) toggleReplayPlayback();
        });
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
        replayTimer.stop();
        controller.dispose();
    }

    // ---- GameController.Listener ----

    @Override
    public void onStateChanged() {
        if (!model.isOver()) exitReplay();
        if (model.getMoveCount() > lastMoveCount) SoundManager.play(SoundManager.CLICK);
        lastMoveCount = model.getMoveCount();

        undo.setEnabled(controller.canUndo());
        hint.setEnabled(controller.acceptsInput());
        resign.setEnabled(inProgress());
        if (model.getMoveCount() == 0) hintsUsed = 0;
        board.stateChanged();
        lastTickSecond = -1;
        showCountdown(controller.getRemainingMillis());
        refreshStatus();
    }

    @Override
    public void onGameOver() {
        int winner = model.getWinner();
        if (winner == GameModel.EMPTY) draws++;
        else scores[seatOf(winner)]++;
        refreshScores();
        Stats.Outcome outcome = winner == GameModel.EMPTY ? Stats.Outcome.DRAW
                : seatOf(winner) == 0 ? Stats.Outcome.WIN : Stats.Outcome.LOSS;
        statsBeforeLastGame = Stats.get().record(Stats.bucket(config), outcome, model.getMoveCount());
        refreshStatus();

        boolean humanLost = mode == GameMode.VS_AI && winner == controller.getAiSide();
        SoundManager.play(winner == GameModel.EMPTY || humanLost ? SoundManager.ALERT : SoundManager.WIN);

        // Give the player a moment to see the winning line before the dialog covers it
        gameOverTimer = new Timer(GAME_OVER_DELAY_MS, e -> showGameOverDialog());
        gameOverTimer.setRepeats(false);
        gameOverTimer.start();
    }

    @Override
    public void onClockTick(long remainingMs) {
        showCountdown(remainingMs);
        long second = (remainingMs + 999) / 1000;
        // Tick during the last five seconds, once per second
        if (second <= 5 && second > 0 && second != lastTickSecond) SoundManager.play(SoundManager.HOVER);
        lastTickSecond = second;
    }

    private void showCountdown(long remainingMs) {
        int current = seatOf(model.getCurrentPlayer());
        for (int seat = 0; seat < 2; seat++) {
            cards[seat].setCountdown(seat == current ? remainingMs : -1, controller.getTurnSeconds());
        }
    }

    @Override
    public void onAiThinking(boolean thinking) {
        hint.setEnabled(controller.acceptsInput());
        refreshStatus();
    }

    // ---- Actions ----

    private void showGameOverDialog() {
        gameOverTimer = null;
        if (!isShowing() || !model.isOver()) return;
        int option = CustomDialog.confirm(window, resultTitle(), scoreLine(), I18n.t("game.over.playAgain"),
                I18n.t("game.over.viewBoard"), false);
        if (option == CustomDialog.YES_OPTION) controller.newRound(config.alternateFirst());
        else enterReplay();
    }

    // ---- Replay ----

    private void enterReplay() {
        if (!model.isOver() || replayBar.isVisible()) return;
        replayMoves = model.getMoves();
        replayIndex = replayMoves.size();
        replayBar.setRange(replayMoves.size());
        replayBar.setPosition(replayIndex);
        replayBar.setPlaying(false);
        replayBar.setVisible(true);
        revalidate();
    }

    /** Opens the replay if the round is over; false while a game is still running. */
    private boolean ensureReplay() {
        if (!model.isOver()) return false;
        enterReplay();
        return true;
    }

    private void replayStep(java.util.function.IntSupplier target) {
        if (!ensureReplay()) return;
        stopReplayPlayback();
        seekReplay(target.getAsInt());
    }

    private void exitReplay() {
        if (!replayBar.isVisible()) return;
        stopReplayPlayback();
        board.setDisplayModel(null);
        replayBar.setVisible(false);
        revalidate();
    }

    private void seekReplay(int k) {
        if (replayMoves == null) return;
        replayIndex = Math.max(0, Math.min(k, replayMoves.size()));
        // The last step is the live final position, which also carries the winning line
        board.setDisplayModel(replayIndex == replayMoves.size() ? null
                : GameModel.replay(model.getSize(), replayMoves, replayIndex));
        replayBar.setPosition(replayIndex);
        if (replayIndex >= replayMoves.size()) stopReplayPlayback();
    }

    private void toggleReplayPlayback() {
        if (replayMoves == null) return;
        if (replayTimer.isRunning()) {
            stopReplayPlayback();
            return;
        }
        if (replayIndex >= replayMoves.size()) seekReplay(0);
        replayTimer.start();
        replayBar.setPlaying(true);
    }

    private void stopReplayPlayback() {
        replayTimer.stop();
        replayBar.setPlaying(false);
    }

    private void toggleMoveNumbers() {
        boolean on = !board.isShowingMoveNumbers();
        board.setShowMoveNumbers(on);
        numbers.setVariant(on ? AppButton.Variant.PRIMARY : AppButton.Variant.SECONDARY);
    }

    private void showHint() {
        if (!controller.acceptsInput()) return;
        hint.setEnabled(false);
        controller.requestHint(move -> {
            hintsUsed++;
            board.flashHint(move[0], move[1]);
            hint.setEnabled(controller.acceptsInput());
        });
    }

    private void undoMove() {
        // Taking back the winning move also takes back the point it scored
        if (model.isOver()) {
            int winner = model.getWinner();
            if (winner == GameModel.EMPTY) draws--;
            else scores[seatOf(winner)]--;
            refreshScores();
            if (statsBeforeLastGame != null) {
                Stats.get().restore(Stats.bucket(config), statsBeforeLastGame);
                statsBeforeLastGame = null;
            }
        }
        cancelGameOverDialog();
        controller.undo();
    }

    private void requestResign() {
        if (!inProgress()) return;
        int loser = mode == GameMode.VS_AI ? controller.getHumanSide() : model.getCurrentPlayer();
        controller.pauseClock();
        int option = CustomDialog.confirm(window, I18n.t("game.resign.title", nameOf(loser)),
                I18n.t("game.resign.message"), I18n.t("game.resign.confirm"), I18n.t("common.cancel"), true);
        controller.resumeClock();
        if (option == CustomDialog.YES_OPTION) controller.resign();
    }

    private void requestRestart() {
        if (inProgress()) {
            controller.pauseClock();
            int option = CustomDialog.confirm(window, I18n.t("game.restart.title"),
                    I18n.t("game.restart.message"), I18n.t("game.restart.confirm"), I18n.t("common.cancel"), false);
            controller.resumeClock();
            if (option != CustomDialog.YES_OPTION) return;
        }
        cancelGameOverDialog();
        // After a finished round "restart" means the next round; mid-game it replays the same one
        if (model.isOver()) controller.newRound(config.alternateFirst());
        else controller.restart();
    }

    private void requestLeave() {
        if (inProgress()) {
            controller.pauseClock();
            int option = CustomDialog.confirm(window, I18n.t("game.leave.title"),
                    I18n.t("game.leave.message"), I18n.t("game.leave.confirm"), I18n.t("game.leave.stay"), true);
            controller.resumeClock();
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
        if (model.getEndReason() == EndReason.TIMEOUT) return I18n.t("game.result.timeout", nameOf(winner));
        if (model.getEndReason() == EndReason.RESIGN) {
            return I18n.t("game.result.resigned", nameOf(GameModel.opponent(winner)), nameOf(winner));
        }
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
        if (hintsUsed > 0) line += "\n" + I18n.t("game.result.hints", hintsUsed);
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
        boolean icon = text.length() == 1;
        b.setFont(Theme.semibold(icon ? 18f : 13f));
        // Text buttons measure themselves so translations never get clipped
        if (icon) b.setPreferredSize(new Dimension(44, 44));
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
