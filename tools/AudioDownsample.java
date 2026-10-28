import javax.sound.sampled.*;
import java.io.File;

/**
 * Re-encodes a WAV file as 16-bit PCM at a lower sample rate / channel count.
 * Usage: java tools/AudioDownsample.java <in.wav> <out.wav> [sampleRate=22050] [channels=1]
 */
public class AudioDownsample {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java tools/AudioDownsample.java <in.wav> <out.wav> [sampleRate] [channels]");
            System.exit(1);
        }
        float rate = args.length > 2 ? Float.parseFloat(args[2]) : 22050f;
        int channels = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        try (AudioInputStream in = AudioSystem.getAudioInputStream(new File(args[0]))) {
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, channels,
                    channels * 2, rate, false);
            try (AudioInputStream out = AudioSystem.getAudioInputStream(target, in)) {
                AudioSystem.write(out, AudioFileFormat.Type.WAVE, new File(args[1]));
            }
        }
        System.out.printf("%s -> %s (%.0f Hz, %d ch)%n", args[0], args[1], rate, channels);
    }
}
