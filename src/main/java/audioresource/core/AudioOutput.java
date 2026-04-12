package audioresource.core;

/**
 * Abstraction for a hardware audio output (e.g., speakers) that accepts PCM byte data.
 * <p>
 * Implementations are responsible for opening the audio line, writing raw audio bytes,
 * pausing/resuming playback, flushing pending data, and closing the line.
 * </p>
 *
 * @author Salmane Khalili
 * @see JavaSoundAdapter
 */
public interface AudioOutput {

    /**
     * Opens and starts the audio output line.
     * Must be called before {@link #write(byte[], int, int)}.
     */
    void play();

    /**
     * Writes PCM bytes to the audio output.
     *
     * @param buffer source buffer
     * @param offset start offset in the buffer
     * @param length number of bytes to write
     * @return the number of bytes actually written (typically equals {@code length})
     */
    int write(byte[] buffer, int offset, int length);

    /**
     * Waits for all pending data to be played before returning.
     */
    void drain();

    /**
     * Pauses the audio output (data can still be written but will not be played).
     */
    void pause();

    /**
     * Discards any currently queued data in the output line.
     */
    void flush();

    /**
     * Releases the audio line and any associated system resources.
     */
    void close();
}