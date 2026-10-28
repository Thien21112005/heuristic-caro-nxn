package model;

import model.ai.Difficulty;

/**
 * Everything chosen on the setup screen for one game session.
 */
public record GameConfig(GameMode mode, int size, Difficulty difficulty) {

    public static GameConfig twoPlayers(int size) {
        return new GameConfig(GameMode.TWO_PLAYERS, size, Difficulty.NORMAL);
    }

    public static GameConfig vsAi(int size, Difficulty difficulty) {
        return new GameConfig(GameMode.VS_AI, size, difficulty);
    }
}
