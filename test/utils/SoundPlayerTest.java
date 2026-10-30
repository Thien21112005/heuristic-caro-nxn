package utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoundPlayerTest {
    private static final float MIN = -80f, MAX = 6f;

    @Test
    void fullVolumeIsZeroDecibels() {
        assertEquals(0f, SoundPlayer.toDecibels(100, MIN, MAX), 0.001f);
    }

    @Test
    void halfVolumeIsAboutMinusSixDecibels() {
        assertEquals(-6.02f, SoundPlayer.toDecibels(50, MIN, MAX), 0.01f);
    }

    @Test
    void zeroIsSilentAndValuesAreClamped() {
        assertEquals(MIN, SoundPlayer.toDecibels(0, MIN, MAX));
        assertEquals(MIN, SoundPlayer.toDecibels(-5, MIN, MAX));
        assertEquals(0f, SoundPlayer.toDecibels(250, MIN, MAX), 0.001f);
        assertEquals(-20f, SoundPlayer.toDecibels(1, -20f, MAX), "never below the control's minimum");
    }
}
