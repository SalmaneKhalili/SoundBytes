package audioresource.buffer;

/**
 * A fixed‑size, thread‑unsafe byte ring buffer (circular buffer).
 * <p>
 * Serves as an intermediate store between a decoder (producer) and an audio output (consumer).
 * Supports single‑byte and bulk write/read operations, tracks available space, and can be cleared.
 * </p>
 * <p>
 * <b>Note:</b> Not thread‑safe; external synchronization is required for concurrent access.
 * </p>
 *
 * @author Salmane Khalili
 */
public class RingBuffer {

    private final int capacity;
    private final byte[] buffer;
    private int lastInsert = 0;
    private int lastRead = 0;

    private int size = 0;

    /**
     * Constructs a new ring buffer with the given capacity.
     *
     * @param len the maximum number of bytes the buffer can hold
     */
    public RingBuffer(int len) {
        capacity = len;
        buffer = new byte[len];
    }

    /**
     * Returns the total capacity of the buffer.
     *
     * @return the maximum number of bytes that can be stored
     */
    public int getCapacity() {
        return this.capacity;
    }

    /**
     * Writes a single byte into the buffer.
     *
     * @param b the byte to write
     * @return {@code true} if the byte was written, {@code false} if the buffer is full
     */
    public boolean write(byte b) {
        if (size == capacity) {
            return false;
        }
        buffer[lastInsert] = b;
        lastInsert = (lastInsert + 1) % capacity;
        size++;
        return true;
    }

    /**
     * Writes a sequence of bytes from a source array into the buffer.
     * Writes until either all bytes are written or the buffer becomes full.
     *
     * @param src    the source byte array
     * @param offset starting offset in the source array
     * @param length maximum number of bytes to write
     * @return the actual number of bytes written
     */
    public int write(byte[] src, int offset, int length) {
        int written = 0;
        while (written < length && size < capacity) {
            buffer[lastInsert] = src[offset + written];
            lastInsert = (lastInsert + 1) % capacity;
            written++;
            size++;
        }
        return written;
    }

    /**
     * Reads a single byte from the buffer and returns it as an unsigned value (0‑255).
     *
     * @return the next byte (converted to int in range 0‑255), or -1 if the buffer is empty
     */
    public int read() {
        if (size == 0) {
            return -1;
        }
        byte b = buffer[lastRead];
        lastRead = (lastRead + 1) % capacity;
        size--;
        // Convert signed byte to unsigned int (0-255)
        return b & 0xFF;
    }

    /**
     * Reads up to {@code length} bytes from the buffer into a destination array.
     *
     * @param destination the array to write into
     * @param offset      starting offset in the destination array
     * @param length      maximum number of bytes to read
     * @return the actual number of bytes read
     */
    public int read(byte[] destination, int offset, int length) {
        int read = 0;
        while (read < length && size > 0) {
            destination[offset + read] = buffer[lastRead];
            lastRead = (lastRead + 1) % capacity;
            read++;
            size--;
        }
        return read;
    }

    /** Removes all data from the buffer, resetting it to an empty state. */
    public void clear() {
        lastInsert = 0;
        lastRead = 0;
        size = 0;
    }

    /**
     * Returns the number of bytes currently available for reading.
     *
     * @return the number of bytes stored in the buffer
     */
    public int available() {
        return size;
    }

    /**
     * Returns the number of free bytes that can still be written.
     *
     * @remaining the remaining free space
     */
    public int remaining() {
        return capacity - size;
    }

    /**
     * Checks whether the buffer is empty.
     *
     * @return {@code true} if no bytes are stored, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Checks whether the buffer is full.
     *
     * @return {@code true} if no more bytes can be written, {@code false} otherwise
     */
    public boolean isFull() {
        return size == capacity;
    }
}