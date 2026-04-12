package audioresource.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * {@link AudioOutput} implementation using the standard Java Sound API
 * ({@link SourceDataLine}).
 * <p>
 * Translates the generic {@code AudioOutput} methods into concrete calls on
 * a {@code SourceDataLine}. The line must be obtained beforehand (e.g., from
 * {@link javax.sound.sampled.AudioSystem#getSourceDataLine(AudioFormat)}).
 * </p>
 *
 * @author Salmane Khalili
 */
public class JavaSoundAdapter implements AudioOutput {
    private final SourceDataLine dataLine;
    private final AudioFormat audioFormat;

    /**
     * Constructs a new adapter.
     *
     * @param dataLine   the opened (but not yet started) source data line
     * @param audioFormat format of the PCM data to be written
     */
    public JavaSoundAdapter(SourceDataLine dataLine, AudioFormat audioFormat) {
        this.dataLine = dataLine;
        this.audioFormat = audioFormat;
    }

    /**
     * Opens and starts the source data line.
     *
     * @throws RuntimeException if the line cannot be opened
     */
    @Override
    public void play() {
        try {
            dataLine.open();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        dataLine.start();
    }

    /**
     * Writes PCM bytes to the audio output.
     *
     * @param buffer source buffer
     * @param offset start offset in the buffer
     * @param length number of bytes to write
     * @return the number of bytes written (always {@code length} in this implementation)
     */
    @Override
    public int write(byte[] buffer, int offset, int length) {
        return dataLine.write(buffer, offset, length);
    }

    /** Flushes any buffered data from the line (discards it). */
    @Override
    public void drain() {
        dataLine.flush();
    }

    /** Stops the audio line (pauses playback). */
    @Override
    public void pause() {
        dataLine.stop();
    }

    /** Waits for all currently queued data to be played (drains the line). */
    @Override
    public void flush() {
        dataLine.drain();
    }

    /**
     * Closes the audio line, releasing system resources.
     * Waits for any remaining data to be played before closing.
     */
    @Override
    public void close() {
        dataLine.drain();
        dataLine.stop();
        dataLine.close();
    }
}