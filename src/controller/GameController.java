package controller;

import model.GameMode;
import model.GameModel;
import model.ai.AiStrategy;
import model.ai.Difficulty;

import javax.swing.Timer;
import java.util.Random;

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
    private final Random random = new Random();
    private Listener listener;
    private Timer aiTimer;

    public GameController(int size, GameMode mode) {
        this.model = new GameModel(size);
        this.mode = mode;
        this.ai = mode == GameMode.VS_AI ? Difficulty.NORMAL.createAi() : null;
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public GameModel getModel() { return model; }
    public GameMode getMode() { return mode; }
    public boolean isAiThinking() { return aiTimer != null; }

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
        int delay = MIN_AI_DELAY + random.nextInt(MAX_AI_DELAY - MIN_AI_DELAY + 1);
        aiTimer = new Timer(delay, e -> {
            aiTimer = null;
            if (listener != null) listener.onAiThinking(false);
            if (model.isOver() || model.getCurrentPlayer() != AI_PLAYER) return;
            int[] move = ai.chooseMove(model.getBoard(), AI_PLAYER);
            if (model.play(move[0], move[1])) afterMove();
        });
        aiTimer.setRepeats(false);
        aiTimer.start();
        if (listener != null) listener.onAiThinking(true);
    }

    private void cancelAi() {
        if (aiTimer != null) {
            aiTimer.stop();
            aiTimer = null;
            if (listener != null) listener.onAiThinking(false);
        }
    }

    private void afterMove() {
        fireStateChanged();
        if (model.isOver() && listener != null) listener.onGameOver();
    }

    private void fireStateChanged() {
        if (listener != null) listener.onStateChanged();
    }
}
