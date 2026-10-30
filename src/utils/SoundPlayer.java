package utils;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class SoundPlayer {
    private URL soundFileUrl;
    private Clip clip;

    public SoundPlayer(String relativePath) {
        this.soundFileUrl = ResourceUtils.getResource(relativePath);
    }

    public void playLoop(int volume) {
        if (soundFileUrl == null) return;
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFileUrl);
            clip = AudioSystem.getClip();
            clip.open(audioIn);
            applyVolume(clip, volume);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /** Changes the volume (0–100) of the looping clip while it plays. */
    public void setVolume(int volume) {
        if (clip != null) applyVolume(clip, volume);
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    public void playOnce(int volume) {
        if (soundFileUrl == null || volume <= 0) return;
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFileUrl);
            Clip oneShot = AudioSystem.getClip();
            oneShot.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    oneShot.close();
                }
            });
            oneShot.open(audioIn);
            applyVolume(oneShot, volume);
            oneShot.start();
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /** Maps 0–100 onto the clip's master gain in decibels (perceived loudness is logarithmic). */
    static float toDecibels(int volume, float min, float max) {
        if (volume <= 0) return min;
        float db = (float) (20 * Math.log10(Math.min(100, volume) / 100.0));
        return Math.max(min, Math.min(max, db));
    }

    private static void applyVolume(Clip clip, int volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gain.setValue(toDecibels(volume, gain.getMinimum(), gain.getMaximum()));
    }
}
