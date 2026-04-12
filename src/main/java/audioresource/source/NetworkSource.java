package audioresource.source;

import java.nio.ByteBuffer;

/**
 * Placeholder for a network‑based audio source (e.g., HTTP streaming).
 * <p>
 * Currently not fully implemented – all methods either do nothing or return dummy values.
 * Future versions may support reading from a URL or socket.
 * </p>
 *
 * @author Salmane Khalili
 */
public class NetworkSource implements Source {

    /** Does nothing in this placeholder. */
    @Override
    public void open() {
        // Not implemented
    }

    /**
     * Placeholder read – always returns 0.
     *
     * @param buffer ignored
     * @return 0
     */
    @Override
    public int read(ByteBuffer buffer) {
        return 0;
    }

    /**
     * Placeholder seek – does nothing and returns 0.
     *
     * @param bytePosition ignored
     * @return 0
     */
    @Override
    public long seek(long bytePosition) {
        return 0;
    }

    /**
     * Placeholder getPosition – returns 0.
     *
     * @return 0
     */
    @Override
    public long getPosition() {
        return 0;
    }
}