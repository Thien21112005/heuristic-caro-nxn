package utils;

import model.Settings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry point for all audio. Respects the user's sound settings and
 * loads clips on a background thread so the UI never stutters.
 */
public final class SoundManager {
    public static final String HOVER = "/assets/Scroll.wav";
    public static final String CLICK = "/assets/Select.wav";
    public static final String ALERT = "/assets/Alert.wav";
    public static final String WIN = "/assets/Congratulation.wav";
    private static final String MUSIC = "/assets/bgmusic.wav";

    private static final ExecutorService AUDIO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audio");
        t.setDaemon(true);
        return t;
    });

    private static SoundPlayer music;

    private SoundManager() {}

    public static void play(String path) {
        if (!Settings.get().isSfxEnabled()) return;
        AUDIO.execute(() -> new SoundPlayer(path).playOnce());
    }

    public static void startMusic() {
        if (!Settings.get().isMusicEnabled()) return;
        AUDIO.execute(() -> {
            if (music == null) {
                music = new SoundPlayer(MUSIC);
                music.playLoop();
            }
        });
    }

    public static void stopMusic() {
        AUDIO.execute(() -> {
            if (music != null) {
                music.stop();
                music = null;
            }
        });
    }
}
