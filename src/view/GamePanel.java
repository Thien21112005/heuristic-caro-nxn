package view;

import utils.I18n;
import controller.GameController;
import controller.GameSession;
import controller.NetworkGameController;
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
 * board, and a fixed-height footer that holds the status, the result of the
 * round or the replay controls, so the board never moves or gets covered.
 */
public class GamePanel extends PaperPanel implements GameSession.Listener {
    // Lets the red pen finish its loop around the winning line before the result appears
    private static final int RESULT_DELAY_MS = 500;
    private static final int FOOTER_HEIGHT = 84;
    private static final int HINT_MIN_WIDTH = 960;

    private final MainWindow window;
    private final GameSession controller;
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
    private final AppButton restart;
    private final AppButton numbers;
    private final AppButton replayNumbers;
    private final boolean hintAllowed;

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
    private Timer resultTimer;

    // Footer: one row at a time, always the same height
    private final CardLayout footerCards = new CardLayout();
    private final JPanel footerRow = new JPanel(footerCards);
    private final JLabel resultFace = new JLabel();
    private final JLabel resultTitleLabel = new JLabel();
    private final JLabel resultLine = new JLabel();
    private final AppButton playAgain = new AppButton(I18n.t("game.over.playAgain"), AppButton.Variant.PRIMARY);
    private boolean resultShown;
    private boolean inReplay;
    private String statusBase = "";
    private Color statusColor = Theme.TEXT;
    private long statusClockMs = -1;

    public GamePanel(MainWindow window, GameConfig config) {
        this(window, config, new GameController(config));
    }

    /** Plays an already prepared session (e.g. a connected LAN game). */
    public GamePanel(MainWindow window, GameConfig config, GameSession session) {
                this.window = window;
        this.config = config;
        this.mode = config.mode();
        int size = config.size();
        this.controller = session;
        this.model = controller.getModel();
        setLayout(new BorderLayout());

        cards[0] = new PlayerCard(sideOf(0), seatName(0));
        cards[1] = new PlayerCard(sideOf(1), seatName(1));
        String level = mode == GameMode.VS_AI ? difficultyLabel(config.difficulty())
                : mode == GameMode.LAN ? I18n.t("game.lan.vs", seatName(1)) : null;
        if (mode == GameMode.VS_AI) cards[1].setTag(level);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(10, 48, 4, 20));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(level == null ? mode.getLabel() : mode.getLabel() + "  ·  " + level);
        title.setFont(Theme.handBold(Theme.H3));
        title.setForeground(Theme.TEXT);
        int target = model.getTarget();
        JLabel subtitle = new JLabel(I18n.t("game.subtitle", size, target));
        subtitle.setFont(Theme.font(Font.PLAIN, Theme.SMALL));
        subtitle.setForeground(Theme.TEXT_MUTED);
        titles.add(title);
        titles.add(subtitle);

        AppButton menu = TopBar.backButton(I18n.t("common.back"), this::requestLeave);
        undo = headerButton(I18n.t("game.undo"), I18n.t("game.undo.tooltip"));
        // Taking moves back or asking the engine would be unfair against a person on another machine
        undo.setVisible(Settings.get().isUndoEnabled() && mode != GameMode.LAN);
        undo.addActionListener(e -> undoMove());
        hint = headerButton(I18n.t("game.hint"), I18n.t("game.hint.tooltip"));
        hint.addActionListener(e -> showHint());
        hintAllowed = Settings.get().isHintsEnabled() && mode != GameMode.LAN;
        hint.setVisible(hintAllowed);
        resign = new AppButton(I18n.t("game.resign"), AppButton.Variant.GHOST).setAccent(Theme.DANGER);
        resign.compact();
        resign.setToolTipText(I18n.t("game.resign.tooltip"));
        resign.addActionListener(e -> requestResign());
        restart = headerButton(I18n.t("game.restart"), I18n.t("game.restart.tooltip"));
        restart.addActionListener(e -> requestRestart());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(resign);
        actions.add(hint);
        actions.add(undo);
        actions.add(restart);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(menu);
        left.add(titles);

        header.add(left, BorderLayout.WEST);
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

        replayBar.setOnSeek(this::seekReplay);
        replayBar.setOnTogglePlay(this::toggleReplayPlayback);
        replayTimer = new Timer(REPLAY_STEP_MS, e -> {
            if (replayIndex >= replayMoves.size()) stopReplayPlayback();
            else seekReplay(replayIndex + 1);
        });

        JPanel center = new JPanel(new BorderLayout(20, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 48, 0, 20));
        center.add(wrapCard(cards[0]), BorderLayout.WEST);
        center.add(scroll, BorderLayout.CENTER);
        center.add(wrapCard(cards[1]), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // Status row
        status.setFont(Theme.hand(Theme.LEAD));
        status.setForeground(Theme.TEXT);

        JLabel hintText = new JLabel("<html>" + I18n.t("game.hint.controls") + "</html>");
        hintText.setFont(Theme.font(Font.PLAIN, Theme.SMALL));
        hintText.setForeground(Theme.TEXT_MUTED);

        AppButton zoomOut = zoomButton("−", I18n.t("game.zoomOut.tooltip"), () -> zoomBy(-1));
        AppButton fit = zoomButton(I18n.t("game.fit"), I18n.t("game.fit.tooltip"), () -> {
            autoFit = true;
            fitBoard();
        });
        AppButton zoomIn = zoomButton("+", I18n.t("game.zoomIn.tooltip"), () -> zoomBy(1));
        numbers = zoomButton(I18n.t("game.numbers"), I18n.t("game.numbers.tooltip"), this::toggleMoveNumbers);
        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoom.setOpaque(false);
        zoom.add(numbers);
        zoom.add(zoomOut);
        zoom.add(fit);
        zoom.add(zoomIn);

        JPanel playRow = new JPanel(new BorderLayout(12, 0));
        playRow.setOpaque(false);
        playRow.add(hintText, BorderLayout.WEST);
        playRow.add(status, BorderLayout.CENTER);
        playRow.add(centeredVertically(zoom), BorderLayout.EAST);
        // On a small window the status line needs that room more than the reminder does
        playRow.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                hintText.setVisible(playRow.getWidth() >= HINT_MIN_WIDTH);
            }
        });

        // Result row
        resultTitleLabel.setFont(Theme.handBold(Theme.H2));
        resultTitleLabel.setForeground(Theme.TEXT);
        resultLine.setFont(Theme.font(Font.PLAIN, Theme.BODY));
        resultLine.setForeground(Theme.TEXT_MUTED);
        JPanel resultText = new JPanel();
        resultText.setOpaque(false);
        resultText.setLayout(new BoxLayout(resultText, BoxLayout.Y_AXIS));
        resultText.add(resultTitleLabel);
        resultText.add(resultLine);
        playAgain.setFont(Theme.hand(Theme.LEAD));
        playAgain.addActionListener(e -> {
            controller.newRound(config.alternateFirst());
            if (model.isOver()) refreshResult();
        });
        AppButton review = new AppButton(I18n.t("game.over.viewBoard"), AppButton.Variant.SECONDARY);
        review.addActionListener(e -> enterReplay());
        JPanel resultButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        resultButtons.setOpaque(false);
        resultButtons.add(review);
        resultButtons.add(playAgain);
        JPanel resultRow = new JPanel(new BorderLayout(14, 0));
        resultRow.setOpaque(false);
        resultRow.add(resultFace, BorderLayout.WEST);
        resultRow.add(resultText, BorderLayout.CENTER);
        resultRow.add(centeredVertically(resultButtons), BorderLayout.EAST);

        // Replay row
        AppButton closeReplay = new AppButton(I18n.t("game.replay.close"), AppButton.Variant.SECONDARY);
        closeReplay.addActionListener(e -> exitReplay());
        JPanel replayRow = new JPanel(new BorderLayout(12, 0));
        replayRow.setOpaque(false);
        replayRow.add(centeredVertically(replayBar), BorderLayout.CENTER);
        replayNumbers = zoomButton(I18n.t("game.numbers"), I18n.t("game.numbers.tooltip"), this::toggleMoveNumbers);
        JPanel replayActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        replayActions.setOpaque(false);
        replayActions.add(replayNumbers);
        replayActions.add(closeReplay);
        replayRow.add(centeredVertically(replayActions), BorderLayout.EAST);

        footerRow.setOpaque(false);
        footerRow.setBorder(BorderFactory.createEmptyBorder(6, 48, 8, 20));
        footerRow.setPreferredSize(new Dimension(10, FOOTER_HEIGHT));
        footerRow.add(playRow, "play");
        footerRow.add(resultRow, "result");
        footerRow.add(replayRow, "replay");
        add(footerRow, BorderLayout.SOUTH);

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
        // During play they bring back the board's cursor if a button had taken the focus
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), () -> arrowKey(0, -1, () -> replayIndex - 1));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), () -> arrowKey(0, 1, () -> replayIndex + 1));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), () -> arrowKey(-1, 0, null));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), () -> arrowKey(1, 0, null));
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
        SwingUtilities.invokeLater(board::requestFocusInWindow);
    }

    /** Stops timers; call when the screen is discarded. */
    public void dispose() {
        if (resultTimer != null) resultTimer.stop();
        replayTimer.stop();
        controller.dispose();
    }

    // ---- GameSession.Listener ----

    @Override
    public void onStateChanged() {
        if (!model.isOver()) {
            leaveReplay();
            resultShown = false;
        }
        if (model.getMoveCount() > lastMoveCount) SoundManager.play(SoundManager.CLICK);
        lastMoveCount = model.getMoveCount();

        undo.setEnabled(controller.canUndo());
        hint.setEnabled(controller.acceptsInput());
        boolean linked = !(controller instanceof NetworkGameController lan) || lan.isConnected();
        resign.setEnabled(inProgress() && linked);
        restart.setEnabled(linked);
        // At the end of a round the result row offers what comes next; these have nothing left to do
        boolean over = model.isOver();
        resign.setVisible(!over);
        hint.setVisible(hintAllowed && !over);
        restart.setVisible(!over);
        if (model.getMoveCount() == 0) hintsUsed = 0;
        board.stateChanged();
        lastTickSecond = -1;
        showCountdown(controller.getRemainingMillis());
        refreshStatus();
        updateFooter();
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

        boolean humanLost = mode == GameMode.VS_AI && winner == GameModel.opponent(controller.getHumanSide());
        SoundManager.play(winner == GameModel.EMPTY || humanLost ? SoundManager.ALERT : SoundManager.WIN);

        resultTimer = new Timer(RESULT_DELAY_MS, e -> revealResult());
        resultTimer.setRepeats(false);
        resultTimer.start();
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
        // The same clock in words, right under the board where the player is looking
        statusClockMs = controller.getTurnSeconds() > 0 ? remainingMs : -1;
        paintStatus();
    }

    @Override
    public void onAiThinking(boolean thinking) {
        hint.setEnabled(controller.acceptsInput());
        refreshStatus();
    }

    // ---- Actions ----

    /** Shows the outcome below the board: nothing covers the winning line. */
    private void revealResult() {
        resultTimer = null;
        if (!model.isOver()) return;
        resultShown = true;
        updateFooter();
    }

    private void updateFooter() {
        if (inReplay) return;
        boolean showResult = model.isOver() && resultShown;
        if (showResult) refreshResult();
        footerCards.show(footerRow, showResult ? "result" : "play");
    }

    private void refreshResult() {
        int winner = model.getWinner();
        boolean interrupted = model.getEndReason() == EndReason.DISCONNECT;
        resultFace.setIcon(interrupted ? null : new ImageIcon(winner != GameModel.EMPTY
                ? BoardView.pieceImage(winner, 68) : drawFaces()));
        resultTitleLabel.setText(resultTitle());
        boolean linked = !(controller instanceof NetworkGameController lan) || lan.isConnected();
        boolean pending = controller instanceof NetworkGameController lan && lan.isRestartPending();
        resultLine.setText(pending ? I18n.t("lan.restart.waiting", seatName(1)) : scoreLine());
        playAgain.setEnabled(linked && !pending);
    }

    /** A draw belongs to both faces. */
    private static Image drawFaces() {
        java.awt.image.BufferedImage both = new java.awt.image.BufferedImage(104, 68, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = both.createGraphics();
        g2.drawImage(BoardView.pieceImage(GameModel.X, 64), 0, 2, null);
        g2.drawImage(BoardView.pieceImage(GameModel.O, 64), 40, 2, null);
        g2.dispose();
        return both;
    }

    // ---- LAN: new-round handshake ----

    @Override
    public void onRestartRequested() {
        if (!(controller instanceof NetworkGameController lan)) return;
        SoundManager.play(SoundManager.ALERT);
        int option = CustomDialog.confirm(window, I18n.t("lan.restart.title", seatName(1)),
                I18n.t("lan.restart.message"), I18n.t("lan.restart.accept"), I18n.t("lan.restart.decline"), false);
        lan.answerRestart(option == CustomDialog.YES_OPTION);
    }

    @Override
    public void onConnectionLost(String reason) {
        refreshStatus();
        if (!isShowing()) return;
        SoundManager.play(SoundManager.ALERT);
        String key = switch (reason) {
            case "bye" -> "lan.lost.left";
            case "desync" -> "lan.lost.desync";
            default -> "lan.lost.connection";
        };
        // The interrupted round counts for nobody, so it is not added to scores or statistics
        int option = CustomDialog.confirm(window, I18n.t(key, seatName(1)), I18n.t("lan.lost.message"),
                I18n.t("lan.lost.lobby"), I18n.t("game.over.viewBoard"), false);
        if (option == CustomDialog.YES_OPTION) {
            dispose();
            window.showScreen(MainWindow.LAN);
        } else {
            enterReplay();
        }
    }

    @Override
    public void onRestartDeclined() {
        CustomDialog.message(window, I18n.t("lan.restart.declined.title", seatName(1)),
                I18n.t("lan.restart.declined.message"));
    }

    // ---- Replay ----

    private void enterReplay() {
        if (!model.isOver() || inReplay) return;
        inReplay = true;
        replayMoves = model.getMoves();
        replayIndex = replayMoves.size();
        replayBar.setRange(replayMoves.size());
        replayBar.setPosition(replayIndex);
        replayBar.setPlaying(false);
        footerCards.show(footerRow, "replay");
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

    private void arrowKey(int dRow, int dCol, java.util.function.IntSupplier replayTarget) {
        if (model.isOver()) {
            if (replayTarget != null) replayStep(replayTarget);
            return;
        }
        board.requestFocusInWindow();
        board.moveCursor(dRow, dCol);
    }

    private void leaveReplay() {
        if (!inReplay) return;
        stopReplayPlayback();
        board.setDisplayModel(null);
        inReplay = false;
    }

    /** Back from the replay to the result of the round. */
    private void exitReplay() {
        leaveReplay();
        resultShown = true;
        updateFooter();
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
        numbers.setVariant(on ? AppButton.Variant.CIRCLED : AppButton.Variant.SECONDARY);
        replayNumbers.setVariant(on ? AppButton.Variant.CIRCLED : AppButton.Variant.SECONDARY);
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
        cancelResultReveal();
        controller.undo();
    }

    private void requestResign() {
        if (!inProgress()) return;
        int loser = mode == GameMode.TWO_PLAYERS ? model.getCurrentPlayer() : controller.getHumanSide();
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
        cancelResultReveal();
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
        window.showScreen(mode == GameMode.VS_AI ? MainWindow.SETUP_AI
                : mode == GameMode.LAN ? MainWindow.LAN : MainWindow.SETUP_TWO);
    }

    private boolean inProgress() {
        return model.getMoveCount() > 0 && !model.isOver();
    }

    private void cancelResultReveal() {
        if (resultTimer != null) {
            resultTimer.stop();
            resultTimer = null;
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
            // An interrupted LAN round has no real winner, so neither card claims the win
            boolean interrupted = model.getEndReason() == EndReason.DISCONNECT;
            cards[seat].setStatus(interrupted || model.isDraw() ? PlayerCard.Status.WAITING
                    : statusFor(sideOf(seat), current, winner, seatThinking));
        }

        String text;
        Color color = Theme.TEXT;
        if (controller instanceof NetworkGameController lan && lan.isRestartPending()) {
            text = I18n.t("lan.restart.waiting", seatName(1));
            color = Theme.TEXT_MUTED;
        } else if (winner != GameModel.EMPTY) {
            text = resultTitle();
        } else if (model.isDraw()) {
            text = I18n.t("game.status.draw");
        } else if (thinking) {
            text = I18n.t("game.status.aiThinking");
            color = GameModel.opponent(controller.getHumanSide()) == GameModel.X ? Theme.X_COLOR : Theme.O_COLOR;
        } else if (mode == GameMode.VS_AI || (mode == GameMode.LAN && current == controller.getHumanSide())) {
            text = I18n.t("game.status.yourTurn", controller.getHumanSide() == GameModel.X ? "X" : "O");
            color = controller.getHumanSide() == GameModel.X ? Theme.X_COLOR : Theme.O_COLOR;
        } else {
            boolean x = current == GameModel.X;
            text = I18n.t("game.status.turn", nameOf(current), x ? "X" : "O");
            color = x ? Theme.X_COLOR : Theme.O_COLOR;
        }
        statusBase = text;
        statusColor = color;
        paintStatus();
    }

    private void paintStatus() {
        boolean timed = statusClockMs >= 0 && !model.isOver();
        long seconds = (statusClockMs + 999) / 1000;
        status.setText(timed ? statusBase + "  ·  " + I18n.t("setup.turnTime.seconds", seconds) : statusBase);
        status.setForeground(timed && seconds <= 5 ? Theme.DANGER : statusColor);
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
        if (model.getEndReason() == EndReason.DISCONNECT) return I18n.t("lan.result.disconnect", seatName(1));
        if (model.getEndReason() == EndReason.RESIGN) {
            return I18n.t("game.result.resigned", nameOf(GameModel.opponent(winner)), nameOf(winner));
        }
        if (mode == GameMode.VS_AI) {
            return I18n.t(winner == controller.getHumanSide() ? "game.result.youWin" : "game.result.aiWins");
        }
        if (mode == GameMode.LAN && winner == controller.getHumanSide()) return I18n.t("game.result.youWin");
        return I18n.t("game.result.wins", nameOf(winner));
    }

    private void refreshScores() {
        cards[0].setScore(scores[0]);
        cards[1].setScore(scores[1]);
    }

    private String scoreLine() {
        String line = seatName(0) + ": " + scores[0] + "   ·   " + seatName(1) + ": " + scores[1];
        if (draws > 0) line += "   ·   " + I18n.t("game.result.draws", draws);
        if (hintsUsed > 0) line += "   ·   " + I18n.t("game.result.hints", hintsUsed);
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
        if (controller instanceof NetworkGameController lan) {
            return seat == 0 ? I18n.t("player.you") : lan.getOpponentName();
        }
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

    private static AppButton headerButton(String text, String tooltip) {
        AppButton b = new AppButton(text, AppButton.Variant.SECONDARY).compact();
        b.setToolTipText(tooltip);
        return b;
    }

    private static AppButton zoomButton(String text, String tooltip, Runnable action) {
        AppButton b = new AppButton(text, AppButton.Variant.SECONDARY).compact();
        b.setToolTipText(tooltip);
        boolean icon = text.length() == 1;
        if (icon) {
            b.setFont(Theme.semibold(Theme.H3));
            b.setPreferredSize(new Dimension(42, 42));
        }
        // Text buttons measure themselves so translations never get clipped
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JComponent centeredVertically(JComponent c) {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        holder.add(c, gbc);
        return holder;
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
