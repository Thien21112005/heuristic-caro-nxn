package controller;

import model.GameConfig;
import model.GameMode;
import model.GameModel;
import model.ai.AiStrategy;
import model.ai.Difficulty;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Drives a single game: validates human moves, schedules the AI reply,
 * and handles undo/restart. The view only renders and forwards input.
 */
public class GameController {
    public interface Listener {
        /** Board, turn or result changed — redraw. */
        void onStateChanged();
        /** The game just ended (win or draw). */
        void onGameOver();
        void onAiThinking(boolean thinking);
    }

    private static final int MIN_AI_DELAY = 300;
    private static final int MAX_AI_DELAY = 800;

    private final GameModel model;
    private final GameMode mode;
    private final AiStrategy ai;
    private final int humanSide;
    private final int aiSide;
    // One background thread shared by all games; search can take a few hundred ms
    private static final ExecutorService AI_THREAD = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai");
        t.setDaemon(true);
        return t;
    });

    private final Random random = new Random();
    private Listener listener;
    private Timer aiTimer;
    private boolean aiThinking;
    // Bumped whenever a pending AI move becomes invalid (undo, restart, leave)
    private int generation;

    public GameController(int size, GameMode mode) {
        this(size, mode, mode == GameMode.VS_AI ? Difficulty.NORMAL.createAi() : null, GameModel.X);
    }

    public GameController(GameConfig config) {
        this(config.size(), config.mode(),
                config.mode() == GameMode.VS_AI ? config.difficulty().createAi() : null, config.humanSide());
    }

    /** For tests and custom opponents: plays VS AI with the given strategy. */
    GameController(int size, GameMode mode, AiStrategy ai) {
        this(size, mode, ai, GameModel.X);
    }

    GameController(int size, GameMode mode, AiStrategy ai, int humanSide) {
        this.model = new GameModel(size);
        this.mode = mode;
        this.ai = ai;
        this.humanSide = humanSide;
        this.aiSide = GameModel.opponent(humanSide);
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public GameModel getModel() { return model; }
    public GameMode getMode() { return mode; }
    public boolean isAiThinking() { return aiThinking; }
    public int getHumanSide() { return humanSide; }
    public int getAiSide() { return aiSide; }

    /** Call once the listener is attached: lets the AI open when it plays X. */
    public void start() {
        maybeStartAi();
    }

    /** Against the AI only the player's own moves can be taken back. */
    public boolean canUndo() {
        return mode == GameMode.TWO_PLAYERS ? model.canUndo() : model.movesBy(humanSide) > 0;
    }

    /** True when a click on the board should be accepted right now. */
    public boolean acceptsInput() {
        return !model.isOver() && !isAiThinking()
                && (mode == GameMode.TWO_PLAYERS || model.getCurrentPlayer() == humanSide);
    }

    public void play(int r, int c) {
        if (!acceptsInput() || !model.play(r, c)) return;
        afterMove();
        maybeStartAi();
    }

    public void undo() {
        if (!canUndo()) return;
        cancelAi();
        model.undo();
        // Against the AI, roll back to the human's turn so they can actually replay
        while (mode == GameMode.VS_AI && model.getCurrentPlayer() != humanSide && model.canUndo()) {
            model.undo();
        }
        fireStateChanged();
        maybeStartAi();
    }

    public void restart() {
        cancelAi();
        model.reset();
        fireStateChanged();
        maybeStartAi();
    }

    private void maybeStartAi() {
        if (mode == GameMode.VS_AI && !model.isOver() && model.getCurrentPlayer() == aiSide && !aiThinking) {
            scheduleAiMove();
        }
    }

    /** Stops any pending AI move; call when leaving the screen. */
    public void dispose() {
        cancelAi();
        listener = null;
    }

    private void scheduleAiMove() {
        int token = ++generation;
        int minDelay = MIN_AI_DELAY + random.nextInt(MAX_AI_DELAY - MIN_AI_DELAY + 1);
        long start = System.currentTimeMillis();
        int[][] snapshot = copy(model.getBoard());
        aiThinking = true;
        if (listener != null) listener.onAiThinking(true);

        AI_THREAD.execute(() -> {
            int[] move;
            try {
                move = ai.chooseMove(snapshot, aiSide);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                move = firstEmpty(snapshot);
            }
            int[] chosen = move;
            SwingUtilities.invokeLater(() -> {
                if (token != generation) return;
                // Keep a short "thinking" pause even when the engine answers instantly
                int wait = (int) Math.max(0, minDelay - (System.currentTimeMillis() - start));
                aiTimer = new Timer(wait, e -> applyAiMove(token, chosen));
                aiTimer.setRepeats(false);
                aiTimer.start();
            });
        });
    }

    private void applyAiMove(int token, int[] move) {
        if (token != generation) return;
        aiTimer = null;
        aiThinking = false;
        if (listener != null) listener.onAiThinking(false);
        if (model.isOver() || model.getCurrentPlayer() != aiSide) return;
        if (model.play(move[0], move[1])) afterMove();
    }

    private void cancelAi() {
        generation++;
        if (aiTimer != null) {
            aiTimer.stop();
            aiTimer = null;
        }
        if (aiThinking) {
            aiThinking = false;
            if (listener != null) listener.onAiThinking(false);
        }
    }

    private static int[][] copy(int[][] board) {
        int[][] out = new int[board.length][];
        for (int i = 0; i < board.length; i++) out[i] = board[i].clone();
        return out;
    }

    private static int[] firstEmpty(int[][] board) {
        for (int r = 0; r < board.length; r++)
            for (int c = 0; c < board.length; c++)
                if (board[r][c] == GameModel.EMPTY) return new int[]{r, c};
        return new int[]{-1, -1};
    }

    private void afterMove() {
        fireStateChanged();
        if (model.isOver() && listener != null) listener.onGameOver();
    }

    private void fireStateChanged() {
        if (listener != null) listener.onStateChanged();
    }
}
