package model;

import model.ai.Difficulty;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Lifetime results, persisted with java.util.prefs and kept per "bucket":
 * one per AI difficulty plus one for two-player games. Outcomes are always
 * from seat 0's point of view (you, or Player 1).
 */
public final class Stats {
    public enum Outcome { WIN, LOSS, DRAW }

    /** Totals for one bucket. {@code fastestWin} is the fewest moves in a won game, 0 if none. */
    public record Entry(int played, int wins, int losses, int draws,
                        int currentStreak, int bestStreak, int fastestWin) {
        public static final Entry EMPTY = new Entry(0, 0, 0, 0, 0, 0, 0);

        public int winRatePercent() {
            return played == 0 ? 0 : Math.round(100f * wins / played);
        }
    }

    private static final Stats INSTANCE = new Stats(Preferences.userNodeForPackage(Stats.class).node("stats"));
    private static final String TWO_PLAYERS = "TWO_PLAYERS";

    private final Preferences prefs;

    Stats(Preferences prefs) {
        this.prefs = prefs;
    }

    public static Stats get() { return INSTANCE; }

    public static String bucket(GameMode mode, Difficulty difficulty) {
        if (mode == GameMode.LAN) return "LAN";
        return mode == GameMode.TWO_PLAYERS ? TWO_PLAYERS : "VS_AI." + difficulty.name();
    }

    public static String bucket(GameConfig config) {
        return bucket(config.mode(), config.difficulty());
    }

    public Entry get(String bucket) {
        return new Entry(
                prefs.getInt(bucket + ".played", 0),
                prefs.getInt(bucket + ".wins", 0),
                prefs.getInt(bucket + ".losses", 0),
                prefs.getInt(bucket + ".draws", 0),
                prefs.getInt(bucket + ".streak", 0),
                prefs.getInt(bucket + ".bestStreak", 0),
                prefs.getInt(bucket + ".fastestWin", 0));
    }

    /**
     * Adds one finished game.
     * @param moves total moves played in the game (used for "fastest win")
     * @return the entry before this game, so the result can be undone with {@link #restore}
     */
    public Entry record(String bucket, Outcome outcome, int moves) {
        Entry before = get(bucket);
        int wins = before.wins(), losses = before.losses(), draws = before.draws();
        int streak = before.currentStreak(), best = before.bestStreak(), fastest = before.fastestWin();
        switch (outcome) {
            case WIN:
                wins++;
                streak++;
                best = Math.max(best, streak);
                fastest = fastest == 0 ? moves : Math.min(fastest, moves);
                break;
            case LOSS:
                losses++;
                streak = 0;
                break;
            default:
                draws++;
                streak = 0;
        }
        put(bucket, new Entry(before.played() + 1, wins, losses, draws, streak, best, fastest));
        return before;
    }

    /** Puts a bucket back to an earlier state (e.g. when the winning move is undone). */
    public void restore(String bucket, Entry entry) {
        put(bucket, entry);
    }

    public void reset() {
        try {
            prefs.clear();
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void put(String bucket, Entry e) {
        prefs.putInt(bucket + ".played", e.played());
        prefs.putInt(bucket + ".wins", e.wins());
        prefs.putInt(bucket + ".losses", e.losses());
        prefs.putInt(bucket + ".draws", e.draws());
        prefs.putInt(bucket + ".streak", e.currentStreak());
        prefs.putInt(bucket + ".bestStreak", e.bestStreak());
        prefs.putInt(bucket + ".fastestWin", e.fastestWin());
    }
}
