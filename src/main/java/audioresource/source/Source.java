package audioresource.source;

import java.nio.ByteBuffer;

/**
 * Low‑level byte source abstraction for reading audio data from different origins
 * (file, network, etc.).
 * <p>
 * A source supports random access (seek) and sequential reading into a {@link ByteBuffer}.
 * </p>
 *
 * @author Salmane Khalili
 * @see FileSource

 */
public interface Source {

    /** Opens the underlying resource (file, socket, etc.). */
    void open();

    /**
     * Reads bytes from the source into the given buffer.
     *
     * @param buffer the destination buffer (position will be advanced)
     * @return the number of bytes read, or -1 if end of stream is reached
     */
    int read(ByteBuffer buffer);

    /**
     * Moves the current position to the specified byte offset.
     *
     * @param bytePosition absolute byte position from the beginning of the source
     * @return the new position (may differ from {@code bytePosition} if the source is unbuffered)
     */
    long seek(long bytePosition);

    /**
     * Returns the current byte position in the source.
     *
     * @return current position
     */
    long getPosition();

    void close();

    boolean isSeekable();
}