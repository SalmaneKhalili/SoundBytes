package audioresource;

import audioresource.core.AudioEngine;
import audioresource.listener.AudioListener;
import audioresource.dto.AudioStatus;

/**
 * Simple command‑line test harness for the {@link AudioEngine}.
 * <p>
 * Demonstrates loading a WAV file, controlling playback (play/pause/resume/seek),
 * and listening to progress, status changes, and errors.
 * </p>
 *
 * @author Salmane Khalili
 */
public class TestEngine {

    /**
     * Entry point: creates an AudioEngine, attaches a listener, loads a WAV file,
     * exercises playback controls, and waits for the song to finish.
     *
     * @param args command line arguments (not used)
     * @throws Exception if any I/O or audio line error occurs
     */
    public static void main(String[] args) throws Exception {
        try (AudioEngine engine = new AudioEngine()) {

            engine.addListener(new AudioListener() {

                @Override
                public void onProgress(long currentMicros, long totalMicros) {
                    double percent = (double) currentMicros / totalMicros * 100;
                    System.out.printf("\rProgress: %.2f%% (%d/%d micros)", percent, currentMicros, totalMicros);
                }

                @Override
                public void onStatusChanged(AudioStatus status) {
                    System.out.println("\n[STATUS] Engine changed to: " + status);
                }

                @Override
                public void onError(Exception e) {
                    System.err.println("\n[ERROR] Something went wrong: " + e.getMessage());
                }
            });

            System.out.println("Loading file...");
            engine.load("tcp://localhost:8888", "WAV");

            engine.play();



            engine.waitUntilFinished();
        }
        System.out.println("\nPlayback complete.");
    }
}