package model;

public enum GameMode {
    VS_AI("VS AI"),
    TWO_PLAYERS("Two Players");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
