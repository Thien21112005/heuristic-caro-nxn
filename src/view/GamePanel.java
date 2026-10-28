package view;

import controller.GameController;
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

    private final BoardView board;
    private final JScrollPane scroll;
    private final PlayerCard cardX;
    private final PlayerCard cardO;
    private final JLabel status = new JLabel("", SwingConstants.CENTER);
    private final AppButton undo;

    private int scoreX, scoreO, draws;
    private int lastMoveCount;
    private boolean autoFit = true;
    private Timer gameOverTimer;

    public GamePanel(MainWindow window, GameMode mode, int size) {
        super("/assets/Background.jpg");
        this.window = window;
        this.mode = mode;
        this.controller = new GameController(size, mode);
        this.model = controller.getModel();
        setLayout(new BorderLayout());

        boolean vsAi = mode == GameMode.VS_AI;
        cardX = new PlayerCard(GameModel.X, vsAi ? "You" : "Player 1");
        cardO = new PlayerCard(GameModel.O, vsAi ? "AI" : "Player 2");

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 6, 20));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(mode.getLabel());
        title.setFont(Theme.font(Font.BOLD, 20f));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        int target = model.getTarget();
        JLabel subtitle = new JLabel(size + " × " + size + "  ·  " + target + " in a row to win");
        subtitle.setFont(Theme.font(Font.PLAIN, 13f));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        titles.add(title);
        titles.add(subtitle);

        AppButton menu = TopBar.backButton("Back", this::requestLeave);
        undo = new AppButton("Undo", AppButton.Variant.SECONDARY);
        undo.setToolTipText("Take back a move (Ctrl+Z)");
        undo.setVisible(Settings.get().isUndoEnabled());
        undo.addActionListener(e -> undoMove());
        AppButton restart = new AppButton("Restart", AppButton.Variant.SECONDARY);
        restart.setToolTipText("Start a new round (R)");
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
        center.add(wrapCard(cardX), BorderLayout.WEST);
        center.add(scroll, BorderLayout.CENTER);
        center.add(wrapCard(cardO), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // Status bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 24, 14, 24));
        status.setFont(Theme.semibold(17f));
        status.setForeground(Theme.TEXT);

        JLabel hint = new JLabel("Scroll to zoom  ·  Drag to pan");
        hint.setFont(Theme.font(Font.PLAIN, 12f));
        hint.setForeground(Theme.TEXT_MUTED);

        AppButton zoomOut = zoomButton("−", "Zoom out (−)", () -> zoomBy(-1));
        AppButton fit = zoomButton("Fit", "Fit board to window (F)", () -> {
            autoFit = true;
            fitBoard();
        });
        AppButton zoomIn = zoomButton("+", "Zoom in (+)", () -> zoomBy(1));
        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoom.setOpaque(false);
        zoom.add(zoomOut);
        zoom.add(fit);
        zoom.add(zoomIn);

        JPanel hintBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        hintBox.setOpaque(false);
        hintBox.add(hint);
        Dimension footerSide = new Dimension(220, zoom.getPreferredSize().height);
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

        undo.setEnabled(model.canUndo());
        board.stateChanged();
        refreshStatus();
    }

    @Override
    public void onGameOver() {
        int winner = model.getWinner();
        if (winner == GameModel.X) scoreX++;
        else if (winner == GameModel.O) scoreO++;
        else draws++;
        cardX.setScore(scoreX);
        cardO.setScore(scoreO);
        refreshStatus();

        boolean humanLost = mode == GameMode.VS_AI && winner == GameModel.O;
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
        int option = CustomDialog.confirm(window, resultTitle(), scoreLine(), "Play again", "View board", false);
        if (option == CustomDialog.YES_OPTION) controller.restart();
    }

    private void undoMove() {
        // Taking back the winning move also takes back the point it scored
        if (model.isOver()) {
            int winner = model.getWinner();
            if (winner == GameModel.X) scoreX--;
            else if (winner == GameModel.O) scoreO--;
            else draws--;
            cardX.setScore(scoreX);
            cardO.setScore(scoreO);
        }
        cancelGameOverDialog();
        controller.undo();
    }

    private void requestRestart() {
        if (inProgress()) {
            int option = CustomDialog.confirm(window, "Restart this round?",
                    "The current board will be cleared. Scores are kept.", "Restart", "Cancel", false);
            if (option != CustomDialog.YES_OPTION) return;
        }
        cancelGameOverDialog();
        controller.restart();
    }

    private void requestLeave() {
        if (inProgress()) {
            int option = CustomDialog.confirm(window, "Leave this game?",
                    "Your current progress and scores will be lost.", "Leave", "Stay", true);
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

        cardX.setStatus(statusFor(GameModel.X, current, winner, false));
        cardO.setStatus(statusFor(GameModel.O, current, winner, thinking));

        String text;
        Color color = Theme.TEXT;
        if (winner != GameModel.EMPTY) {
            text = resultTitle();
            color = Theme.WIN;
        } else if (model.isDraw()) {
            text = "It's a draw";
        } else if (thinking) {
            text = "AI is thinking…";
            color = Theme.O_COLOR;
        } else if (mode == GameMode.VS_AI) {
            text = "Your turn — place an X";
            color = Theme.X_COLOR;
        } else {
            boolean x = current == GameModel.X;
            text = (x ? "Player 1" : "Player 2") + "'s turn — place " + (x ? "an X" : "an O");
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
        if (winner == GameModel.EMPTY) return "It's a draw";
        if (mode == GameMode.VS_AI) return winner == GameModel.X ? "You win!" : "The AI wins";
        return (winner == GameModel.X ? "Player 1" : "Player 2") + " wins!";
    }

    private String scoreLine() {
        String a = mode == GameMode.VS_AI ? "You" : "Player 1";
        String b = mode == GameMode.VS_AI ? "AI" : "Player 2";
        String line = a + "  " + scoreX + " – " + scoreO + "  " + b;
        if (draws > 0) line += "\n" + draws + (draws == 1 ? " draw" : " draws");
        return line;
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
        b.setPreferredSize(new Dimension(text.length() > 1 ? 52 : 40, 40));
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
