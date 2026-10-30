package model;

public enum GameMode {
    VS_AI("mode.vsAi"),
    TWO_PLAYERS("mode.twoPlayers"),
    LAN("mode.lan");

    private final String labelKey;

    GameMode(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabel() { return utils.I18n.t(labelKey); }
}
