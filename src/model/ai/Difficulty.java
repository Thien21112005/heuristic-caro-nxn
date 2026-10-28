package model.ai;

public enum Difficulty {
    EASY, NORMAL, HARD;

    public AiStrategy createAi() {
        switch (this) {
            default:
                return new HeuristicAi();
        }
    }
}
