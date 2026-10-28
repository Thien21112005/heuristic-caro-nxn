package model;

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

    private final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

    private Settings() {}

    public static Settings get() { return INSTANCE; }

    public boolean isMusicEnabled() { return prefs.getBoolean(KEY_MUSIC, true); }
    public void setMusicEnabled(boolean enabled) { prefs.putBoolean(KEY_MUSIC, enabled); }

    public boolean isSfxEnabled() { return prefs.getBoolean(KEY_SFX, true); }
    public void setSfxEnabled(boolean enabled) { prefs.putBoolean(KEY_SFX, enabled); }

    public boolean isUndoEnabled() { return prefs.getBoolean(KEY_UNDO, false); }
    public void setUndoEnabled(boolean enabled) { prefs.putBoolean(KEY_UNDO, enabled); }

    /** Last board size picked for a mode, so the setup screen remembers it. */
    public int getBoardSize(GameMode mode) { return prefs.getInt(KEY_BOARD_SIZE + mode.name(), 15); }
    public void setBoardSize(GameMode mode, int size) { prefs.putInt(KEY_BOARD_SIZE + mode.name(), size); }
}
