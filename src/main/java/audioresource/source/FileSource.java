package audioresource.source;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * {@link Source} implementation that reads from a local file using
 * {@link SeekableByteChannel}.
 * <p>
 * The file is opened when the instance is created and remains open until
 * the garbage collector closes it or the program exits.
 * </p>
 *
 * @author Salmane Khalili
 */
public class FileSource implements Source {
    private final File file;
    private SeekableByteChannel byteChannel;

    /**
     * Constructs a FileSource for the given path.
     *
     * @param filePath path to an existing, readable file
     * @throws IllegalArgumentException if the path does not point to a regular file
     */
    public FileSource(String filePath) {
        File fileObject = new File(filePath);
        if (fileObject.exists() && fileObject.isFile()) {
            this.file = fileObject;
            open();
        } else {
            throw new IllegalArgumentException("Path is not a file or is invalid.");
        }
    }

    /** Opens the file channel for reading. */
    @Override
    public void open() {
        try {
            Path path = file.toPath();
            this.byteChannel = Files.newByteChannel(path,
                    StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads bytes from the file into the buffer.
     *
     * @param buffer the destination buffer
     * @return number of bytes read, or -1 if EOF
     * @throws RuntimeException if an I/O error occurs
     */
    @Override
    public int read(ByteBuffer buffer) {
        try {
            return byteChannel.read(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Seeks to a new byte position in the file.
     *
     * @param bytePosition absolute byte offset from the start of the file
     * @return the new position (should equal {@code bytePosition})
     * @throws RuntimeException if an I/O error occurs
     */
    @Override
    public long seek(long bytePosition) {
        try {
            byteChannel.position(bytePosition);
            return byteChannel.position();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the current file position.
     *
     * @return current byte offset
     * @throws RuntimeException if an I/O error occurs
     */
    @Override
    public long getPosition() {
        try {
            return byteChannel.position();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (byteChannel != null && byteChannel.isOpen()) {
                byteChannel.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean isSeekable(){
        return true;
    }
}