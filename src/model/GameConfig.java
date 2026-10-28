package model;

import model.ai.Difficulty;

/**
 * Everything chosen on the setup screen for one game session.
 */
/**
 * @param humanSide      piece of seat 0 (you / Player 1) in the first round
 * @param alternateFirst swap pieces after every finished round so both sides get to move first
 * @param turnSeconds    time limit per human turn, 0 for none
 */
public record GameConfig(GameMode mode, int size, Difficulty difficulty, int humanSide, boolean alternateFirst,
                         int turnSeconds) {

    public static GameConfig twoPlayers(int size) {
        return twoPlayers(size, false);
    }

    public static GameConfig twoPlayers(int size, boolean alternateFirst) {
        return new GameConfig(GameMode.TWO_PLAYERS, size, Difficulty.NORMAL, GameModel.X, alternateFirst, 0);
    }

    public static GameConfig vsAi(int size, Difficulty difficulty) {
        return vsAi(size, difficulty, GameModel.X, false);
    }

    /** @param humanSide GameModel.X (move first) or GameModel.O (the AI opens) */
    public static GameConfig vsAi(int size, Difficulty difficulty, int humanSide, boolean alternateFirst) {
        return new GameConfig(GameMode.VS_AI, size, difficulty, humanSide, alternateFirst, 0);
    }

    public GameConfig withTurnSeconds(int seconds) {
        return new GameConfig(mode, size, difficulty, humanSide, alternateFirst, seconds);
    }
}
