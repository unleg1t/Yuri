package ddlc.yuri.utils.client;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.InputStream;

public class SoundUtils {
    public static void playSound(String resourceLocation) {
        playSound(resourceLocation, 1.0f);
    }

    public static void playSound(String resourceLocation, float volume) {
        new Thread(() -> {
            try (InputStream raw = getFileFromResourceAsStream("assets/minecraft/yuri/sound/" + resourceLocation);
                 InputStream sound = new java.io.BufferedInputStream(raw);
                 AudioInputStream audioStream = AudioSystem.getAudioInputStream(sound)) {

                Clip clip = AudioSystem.getClip();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

                clip.open(audioStream);
                if (volume != 1.0f && clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                    javax.sound.sampled.FloatControl gainControl = (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                    float clampedVol = Math.max(0.0001f, Math.min(1.0f, volume));
                    float dB = (float) (Math.log10(clampedVol) * 20.0);
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    gainControl.setValue(dB);
                }
                clip.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static InputStream getFileFromResourceAsStream(String fileName) {
        ClassLoader classLoader = SoundUtils.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }
}
