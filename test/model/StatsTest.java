package model;

import model.ai.Difficulty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class StatsTest {
    private Preferences node;
    private Stats stats;
    private final String hard = Stats.bucket(GameMode.VS_AI, Difficulty.HARD);

    @BeforeEach
    void setUp() {
        // A throwaway node so tests never touch the player's real statistics
        node = Preferences.userRoot().node("caro-test-" + UUID.randomUUID());
        stats = new Stats(node);
    }

    @AfterEach
    void tearDown() throws Exception {
        node.removeNode();
    }

    @Test
    void startsEmpty() {
        assertEquals(Stats.Entry.EMPTY, stats.get(hard));
        assertEquals(0, stats.get(hard).winRatePercent());
    }

    @Test
    void countsResultsAndWinRate() {
        stats.record(hard, Stats.Outcome.WIN, 21);
        stats.record(hard, Stats.Outcome.LOSS, 30);
        stats.record(hard, Stats.Outcome.DRAW, 100);
        stats.record(hard, Stats.Outcome.WIN, 15);
        Stats.Entry e = stats.get(hard);
        assertEquals(4, e.played());
        assertEquals(2, e.wins());
        assertEquals(1, e.losses());
        assertEquals(1, e.draws());
        assertEquals(50, e.winRatePercent());
        assertEquals(15, e.fastestWin());
    }

    @Test
    void tracksCurrentAndBestStreak() {
        stats.record(hard, Stats.Outcome.WIN, 9);
        stats.record(hard, Stats.Outcome.WIN, 9);
        stats.record(hard, Stats.Outcome.WIN, 9);
        stats.record(hard, Stats.Outcome.DRAW, 9);
        stats.record(hard, Stats.Outcome.WIN, 9);
        Stats.Entry e = stats.get(hard);
        assertEquals(1, e.currentStreak());
        assertEquals(3, e.bestStreak());
    }

    @Test
    void restoreUndoesARecordedGame() {
        stats.record(hard, Stats.Outcome.WIN, 20);
        stats.record(hard, Stats.Outcome.WIN, 20);
        Stats.Entry before = stats.record(hard, Stats.Outcome.WIN, 11);
        assertEquals(11, stats.get(hard).fastestWin());
        stats.restore(hard, before);
        Stats.Entry e = stats.get(hard);
        assertEquals(2, e.wins());
        assertEquals(2, e.currentStreak());
        assertEquals(2, e.bestStreak());
        assertEquals(20, e.fastestWin());
    }

    @Test
    void bucketsAreIndependentAndResetClearsAll() {
        String two = Stats.bucket(GameMode.TWO_PLAYERS, Difficulty.NORMAL);
        String easy = Stats.bucket(GameMode.VS_AI, Difficulty.EASY);
        stats.record(two, Stats.Outcome.LOSS, 12);
        stats.record(easy, Stats.Outcome.WIN, 12);
        assertEquals(1, stats.get(two).losses());
        assertEquals(0, stats.get(two).wins());
        assertEquals(1, stats.get(easy).wins());
        assertEquals(Stats.Entry.EMPTY, stats.get(hard));
        stats.reset();
        assertEquals(Stats.Entry.EMPTY, stats.get(two));
        assertEquals(Stats.Entry.EMPTY, stats.get(easy));
    }

    @Test
    void twoPlayerBucketIgnoresDifficulty() {
        assertEquals(Stats.bucket(GameMode.TWO_PLAYERS, Difficulty.EASY),
                Stats.bucket(GameMode.TWO_PLAYERS, Difficulty.HARD));
    }
}
