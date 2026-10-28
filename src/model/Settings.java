package model;

import model.ai.Difficulty;

import java.util.prefs.Preferences;

/**
 * User preferences, persisted between launches via java.util.prefs.
 */
public final class Settings {
    private static final Settings INSTANCE = new Settings();

    private static final String KEY_MUSIC = "musicEnabled";
    private static final String KEY_SFX = "sfxEnabled";
    private static final String KEY_UNDO = "undoEnabled";
    private static final String KEY_BOARD_SIZE = "boardSize.";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_HUMAN_SIDE = "humanSide";
    private static final String KEY_ALTERNATE = "alternateFirst.";

    private final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

    private Settings() {}

    public static Settings get() { return INSTANCE; }

    public boolean isMusicEnabled() { return prefs.getBoolean(KEY_MUSIC, true); }
    public void setMusicEnabled(boolean enabled) { prefs.putBoolean(KEY_MUSIC, enabled); }

    public boolean isSfxEnabled() { return prefs.getBoolean(KEY_SFX, true); }
    public void setSfxEnabled(boolean enabled) { prefs.putBoolean(KEY_SFX, enabled); }

    public boolean isUndoEnabled() { return prefs.getBoolean(KEY_UNDO, false); }
    public void setUndoEnabled(boolean enabled) { prefs.putBoolean(KEY_UNDO, enabled); }

    /** "en" or "vi"; defaults to Vietnamese on a Vietnamese system, English otherwise. */
    public String getLanguage() {
        String fallback = "vi".equals(java.util.Locale.getDefault().getLanguage()) ? "vi" : "en";
        return prefs.get(KEY_LANGUAGE, fallback);
    }
    public void setLanguage(String language) { prefs.put(KEY_LANGUAGE, language); }

    public Difficulty getDifficulty() {
        try {
            return Difficulty.valueOf(prefs.get(KEY_DIFFICULTY, Difficulty.NORMAL.name()));
        } catch (IllegalArgumentException e) {
            return Difficulty.NORMAL;
        }
    }
    public void setDifficulty(Difficulty difficulty) { prefs.put(KEY_DIFFICULTY, difficulty.name()); }

    /** GameModel.X or GameModel.O when playing against the AI. */
    public int getHumanSide() {
        return prefs.getInt(KEY_HUMAN_SIDE, GameModel.X) == GameModel.O ? GameModel.O : GameModel.X;
    }
    public void setHumanSide(int side) { prefs.putInt(KEY_HUMAN_SIDE, side); }

    public boolean isAlternateFirst(GameMode mode) { return prefs.getBoolean(KEY_ALTERNATE + mode.name(), false); }
    public void setAlternateFirst(GameMode mode, boolean on) { prefs.putBoolean(KEY_ALTERNATE + mode.name(), on); }

    /** Last board size picked for a mode, so the setup screen remembers it. */
    public int getBoardSize(GameMode mode) { return prefs.getInt(KEY_BOARD_SIZE + mode.name(), 15); }
    public void setBoardSize(GameMode mode, int size) { prefs.putInt(KEY_BOARD_SIZE + mode.name(), size); }
}
