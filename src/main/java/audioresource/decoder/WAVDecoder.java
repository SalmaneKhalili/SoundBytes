package audioresource.decoder;

import audioresource.source.Source;
import audioresource.dto.FMT;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Decoder for Microsoft WAVE (RIFF) files, supporting PCM (uncompressed) audio.
 * <p>
 * Parses the RIFF chunks, extracts the {@code fmt } and {@code data} chunks,
 * and provides sequential PCM reading with seeking. Seek operations align to frame
 * boundaries (blockAlign) to avoid audio glitches. Only little‑endian PCM WAVs are supported.
 * </p>
 *
 * @author Salmane Khalili
 * @see Decoder
 */
public class WAVDecoder implements Decoder {

    private final Source source;
    private FMT fmtData;
    private long audioStartOffset;
    private long audioDataSize;
    private long audioDataRemaining;
    private int fileSize;

    /**
     * Constructs a WAV decoder for the given file.
     *
     * @param filePath path to a valid WAV file
     * @throws Exception if the file is not a valid WAV or an I/O error occurs
     */
    public WAVDecoder(Source source) throws Exception {
        this.source = source;
        ByteBuffer headerBucket = ByteBuffer.allocate(12);
        headerBucket.order(ByteOrder.LITTLE_ENDIAN);

        this.source.read(headerBucket);
        headerBucket.flip();

        if (!"RIFF".equals(readString(headerBucket))) {
            throw new IllegalArgumentException("Not a RIFF file.");
        }

        this.fileSize = headerBucket.getInt();

        if (!"WAVE".equals(readString(headerBucket))) {
            throw new IllegalArgumentException("Not a WAVE file.");
        }

        ByteBuffer walker = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        boolean foundData = false;

        while (!foundData) {
            walker.clear();
            int bytesRead = this.source.read(walker);
            if (bytesRead < 8) break;

            walker.flip();
            String chunkId = readString(walker);
            int chunkSize = walker.getInt();

            if ("fmt ".equals(chunkId)) {
                parseFormat(chunkSize);
            } else if ("data".equals(chunkId)) {
                this.audioStartOffset = this.source.getPosition();
                this.audioDataSize = chunkSize;
                this.audioDataRemaining = chunkSize;
                foundData = true;
            } else {
                long skipAmount = (chunkSize % 2 == 0) ? chunkSize : chunkSize + 1; //RIFF standards require data chunks to start on an even byte, known as data alignment/padding, hence why im adding 1 to the chunksize on odd chunks
                if (source.isSeekable()){
                    this.source.seek(this.source.getPosition() + skipAmount);
                } else {
                    byte[] buffer = new byte[4096];
                    long remaining = skipAmount;
                    while (remaining > 0) {
                        int toRead = (int) Math.min(remaining, buffer.length);
                        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, toRead);
                        source.read(bb);
                        remaining -= toRead;
                    }
                }

            }
        }

        if (fmtData == null || !foundData) {
            throw new IOException("Incomplete WAV file: Missing fmt or data chunk.");
        }
    }

    /**
     * Returns the parsed fmt chunk data.
     *
     * @return the FMT record
     */
    public FMT getFmtData() {
        return fmtData;
    }

    /**
     * Returns the file offset where the audio data chunk begins.
     *
     * @return start offset in bytes
     */
    public long getAudioStartOffset() {
        return audioStartOffset;
    }

    /**
     * Returns the total size of the audio data chunk.
     *
     * @return size in bytes
     */
    public long getAudioDataSize() {
        return audioDataSize;
    }

    /**
     * Returns the total RIFF file size.
     *
     * @return file size in bytes
     */
    public int getFileSize() {
        return fileSize;
    }

    /** Parses the fmt chunk and stores the data in {@link #fmtData}. */
    private void parseFormat(int chunkSize) {
        ByteBuffer fmtBuffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN);
        source.read(fmtBuffer);
        fmtBuffer.flip();

        this.fmtData = new FMT(
                chunkSize,
                fmtBuffer.getShort(),
                fmtBuffer.getShort(),
                fmtBuffer.getInt(),
                fmtBuffer.getInt(),
                fmtBuffer.getShort(),
                fmtBuffer.getShort()
        );
    }

    /** Reads a 4‑byte ASCII string from the buffer. */
    private String readString(ByteBuffer buffer) {
        byte[] bytes = new byte[4];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    /**
     * Seeks to a microsecond position, aligning to the nearest frame boundary.
     *
     * @param microseconds desired time from start of audio
     * @return the actual time (in microseconds) after alignment
     */
    @Override
    public long seek(long microseconds) {
        long offset = (microseconds * fmtData.byteRate()) / 1_000_000L;
        long framedOffset = offset - (offset % fmtData.blockAlign());
        framedOffset = Math.min(framedOffset, audioDataSize);
        audioDataRemaining = audioDataSize - framedOffset;
        source.seek(audioStartOffset + framedOffset);
        return (framedOffset * 1_000_000L) / fmtData.byteRate();
    }

    /**
     * Returns the audio format (sample rate, bit depth, channels, little‑endian, signed).
     *
     * @return the AudioFormat for this WAV file
     */
    @Override
    public AudioFormat getAudioFormat() {
        return new AudioFormat(
                (float) fmtData.sampleRate(),
                fmtData.bitsPerSample(),
                fmtData.numChannels(),
                true,   // signed
                false   // little‑endian
        );
    }

    /**
     * Reads PCM bytes into the provided buffer.
     *
     * @param buffer the destination buffer
     * @return number of bytes read, or -1 if no more audio data remains
     */
    @Override
    public int read(ByteBuffer buffer) {
        if (audioDataRemaining <= 0) {
            return -1;
        }
        int limit = (int) Math.min(audioDataRemaining, buffer.remaining());
        int originalLimit = buffer.limit();
        buffer.limit(buffer.position() + limit);
        try {
            int readSize = source.read(buffer);
            if (readSize > 0) {
                this.audioDataRemaining -= readSize;
            }
            return readSize;
        } finally {
            buffer.limit(originalLimit);
        }
    }

    /**
     * Returns the current byte position in the underlying source.
     *
     * @return current file position
     */
    public long getPosition() {
        return source.getPosition();
    }

    /**
     * Returns the current playback time based on how many bytes have been consumed.
     *
     * @return time in microseconds
     */
    @Override
    public long getCurrentTimeMicros() {
        return ((audioDataSize - audioDataRemaining) * 1_000_000L) / fmtData.byteRate();
    }

    /**
     * Returns the total duration of the audio.
     *
     * @return duration in microseconds
     */
    @Override
    public long getAudioDuration() {
        return (this.audioDataSize * 1_000_000L) / fmtData.byteRate();
    }
}