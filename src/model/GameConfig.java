package model;

import model.ai.Difficulty;

/**
 * Everything chosen on the setup screen for one game session.
 */
public record GameConfig(GameMode mode, int size, Difficulty difficulty, int humanSide) {

    public static GameConfig twoPlayers(int size) {
        return new GameConfig(GameMode.TWO_PLAYERS, size, Difficulty.NORMAL, GameModel.X);
    }

    public static GameConfig vsAi(int size, Difficulty difficulty) {
        return vsAi(size, difficulty, GameModel.X);
    }

    /** @param humanSide GameModel.X (move first) or GameModel.O (the AI opens) */
    public static GameConfig vsAi(int size, Difficulty difficulty, int humanSide) {
        return new GameConfig(GameMode.VS_AI, size, difficulty, humanSide);
    }
}
