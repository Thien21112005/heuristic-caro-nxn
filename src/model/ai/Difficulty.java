package model.ai;

public enum Difficulty {
    EASY, NORMAL, HARD;

    public AiStrategy createAi() {
        switch (this) {
            case EASY:
                return new EasyAi();
            case HARD:
                return new SearchAi();
            default:
                return new HeuristicAi();
        }
    }
}
