package audioresource.core;

import audioresource.controller.PlayBackController;
import audioresource.decoder.Decoder;
import audioresource.decoder.WAVDecoder;
import audioresource.listener.AudioListener;
import audioresource.source.FileSource;
import audioresource.source.Source;
import audioresource.source.TCPSource;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High‑level facade for audio playback.
 * <p>
 * Manages a ring buffer, a decoder, and a playback controller. Provides a simple API
 * to load an audio file, control playback, and monitor progress via {@link AudioListener}.
 * Implements {@link AutoCloseable} to release system resources.
 * </p>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * try (AudioEngine engine = new AudioEngine()) {
 *     engine.load("music.wav", "WAV");
 *     engine.addListener(myListener);
 *     engine.play();
 *     engine.waitUntilFinished();
 * }
 * }</pre>
 *
 * @author Salmane Khalili
 */
public class AudioEngine implements AutoCloseable {


    private final ArrayBlockingQueue<byte[]> queue;
    private Decoder decoder;
    private PlayBackController controller;
    private final List<AudioListener> listeners = new CopyOnWriteArrayList<>();

    /** Creates a new AudioEngine with an empty ring buffer. */
    public AudioEngine() {
            queue = new ArrayBlockingQueue<byte[]>(16);
    }

    /**
     * Loads an audio file and prepares it for playback.
     * Currently only WAV files are supported.
     *
     * @param filePath path to the audio file
     * @param type     format type (e.g., "WAV", "MP3")
     * @throws IllegalArgumentException if the file cannot be loaded or the format is unsupported
     */
    public void load(String filePath, String type) {

            switch (type) {
                case "WAV" -> {
                    try {
                        if (filePath.startsWith("tcp://")) {
                            String[] parts = filePath.substring(6).split(":");
                            String host = parts[0];

                            int port = Integer.parseInt(parts[1]);
                            Source source = new TCPSource(host, port);
                            source.open();
                            decoder = new WAVDecoder(source);
                        } else {
                            Source source = new FileSource(filePath);
                            decoder = new WAVDecoder(source);
                        }
                    } catch (Exception e) {throw new RuntimeException(e);}
                }
                case "MP3" -> throw new IllegalArgumentException("MP3 not supported");
                default -> throw new IllegalArgumentException("Unknown format: " + type);

        }
    }

    /**
     * Starts playback. If another playback was active, it is stopped first.
     *
     * @throws LineUnavailableException if the system audio line cannot be opened
     * @throws IllegalStateException    if no audio file has been loaded
     */
    public void play() throws LineUnavailableException {
        if (decoder == null) {
            throw new IllegalStateException("No audio loaded. Call load() first.");
        }
        if (controller != null) {
            controller.stop();
            controller = null;
        }
        var audioOutput = new JavaSoundAdapter(
                AudioSystem.getSourceDataLine(decoder.getAudioFormat()),
                decoder.getAudioFormat()
        );
        controller = new PlayBackController(decoder, queue, audioOutput);
        for (AudioListener listener : listeners) {
            controller.addListener(listener);
        }
        controller.play();
    }

    /** Pauses playback (position is retained). */
    public void pause() {
        if (controller != null) controller.pause();
    }

    /** Resumes playback after a pause. */
    public void resume() {
        if (controller != null) controller.resume();
    }

    /** Stops playback and releases the current controller. */
    public void stop() {
        if (controller != null) controller.stop();
    }

    /**
     * Seeks to a new absolute position in microseconds.
     *
     * @param microseconds target time from the start of audio
     */
    public void seek(long microseconds) {
        if (controller != null) controller.seek(microseconds);
    }

    /**
     * Blocks the calling thread until playback finishes (naturally or by stop).
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void waitUntilFinished() throws InterruptedException {
        if (controller != null) controller.waitUntilFinished();
    }

    /**
     * Closes the engine and releases all resources.
     * Equivalent to calling {@link #stop()}.
     */
    @Override
    public void close() {
        if (controller != null) controller.close();
    }

    /**
     * Registers a listener for playback events.
     *
     * @param listener the listener to add
     */
    public void addListener(AudioListener listener) {
        listeners.add(listener);
        if (controller != null) controller.addListener(listener);
    }

    /**
     * Returns the current fill percentage of the ring buffer (0‑100).
     *
     * @return buffer fill percent, or 0 if no controller is active
     */
    public int getBufferFillPercent() {
        return controller != null ? controller.getBufferFillPercent() : 0;
    }

    /**
     * Returns the last computed 50th percentile decoder latency (microseconds).
     *
     * @return P50 latency, or 0 if no data yet
     */
    public long getLastP50() {
        return controller != null ? controller.getLastP50() : 0;
    }

    /**
     * Returns the last computed 95th percentile decoder latency (microseconds).
     *
     * @return P95 latency, or 0 if no data yet
     */
    public long getLastP95() {
        return controller != null ? controller.getLastP95() : 0;
    }
}