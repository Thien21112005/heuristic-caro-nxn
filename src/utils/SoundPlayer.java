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

    public void playLoop() {
        if (soundFileUrl == null) return;
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFileUrl);
            clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }

    public void playOnce() {
        if (soundFileUrl == null) return;
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFileUrl);
            clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
