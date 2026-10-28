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

    private static final int AI_PLAYER = GameModel.O;
    private static final int HUMAN_PLAYER = GameModel.X;
    private static final int MIN_AI_DELAY = 300;
    private static final int MAX_AI_DELAY = 800;

    private final GameModel model;
    private final GameMode mode;
    private final AiStrategy ai;
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
        this(size, mode, mode == GameMode.VS_AI ? Difficulty.NORMAL.createAi() : null);
    }

    public GameController(GameConfig config) {
        this(config.size(), config.mode(),
                config.mode() == GameMode.VS_AI ? config.difficulty().createAi() : null);
    }

    /** For tests and custom opponents: plays VS AI with the given strategy. */
    GameController(int size, GameMode mode, AiStrategy ai) {
        this.model = new GameModel(size);
        this.mode = mode;
        this.ai = ai;
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public GameModel getModel() { return model; }
    public GameMode getMode() { return mode; }
    public boolean isAiThinking() { return aiThinking; }

    /** True when a click on the board should be accepted right now. */
    public boolean acceptsInput() {
        return !model.isOver() && !isAiThinking()
                && (mode == GameMode.TWO_PLAYERS || model.getCurrentPlayer() == HUMAN_PLAYER);
    }

    public void play(int r, int c) {
        if (!acceptsInput() || !model.play(r, c)) return;
        afterMove();
        if (!model.isOver() && mode == GameMode.VS_AI) {
            scheduleAiMove();
        }
    }

    public void undo() {
        cancelAi();
        if (!model.canUndo()) return;
        model.undo();
        // Against the AI, roll back to the human's turn so they can actually replay
        if (mode == GameMode.VS_AI && model.getCurrentPlayer() == AI_PLAYER && model.canUndo()) {
            model.undo();
        }
        // If the AI had opened the game, let it move again
        if (mode == GameMode.VS_AI && model.getCurrentPlayer() == AI_PLAYER) {
            scheduleAiMove();
        }
        fireStateChanged();
    }

    public void restart() {
        cancelAi();
        model.reset();
        fireStateChanged();
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
                move = ai.chooseMove(snapshot, AI_PLAYER);
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
        if (model.isOver() || model.getCurrentPlayer() != AI_PLAYER) return;
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
