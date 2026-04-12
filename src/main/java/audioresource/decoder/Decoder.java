package audioresource.decoder;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * Provides a uniform interface for decoding audio files into PCM data.
 * <p>
 * A decoder knows the {@link AudioFormat} of the raw PCM, can read PCM bytes
 * into a {@link ByteBuffer}, supports seeking by microsecond position,
 * and reports current time and total duration.
 * </p>
 *
 * @author Salmane Khalili
 * @see WAVDecoder
 */
public interface Decoder {

    /**
     * Returns the PCM format of the decoded audio.
     *
     * @return the audio format (sample rate, bit depth, channels, endianness)
     */
    AudioFormat getAudioFormat();

    /**
     * Reads PCM data into the given buffer.
     *
     * @param buffer the destination buffer (its position will be advanced)
     * @return the number of bytes read, or -1 if end of audio data has been reached
     */
    int read(ByteBuffer buffer);

    /**
     * Moves the decoding position to the specified microsecond.
     * The actual position may be aligned to a frame boundary.
     *
     * @param microseconds absolute time from the start of the audio
     * @return the actual time (in microseconds) after alignment
     */
    long seek(long microseconds);

    /**
     * Returns the current decoding position.
     *
     * @return position in microseconds
     */
    long getCurrentTimeMicros();

    /**
     * Returns the total duration of the audio.
     *
     * @return duration in microseconds
     */
    long getAudioDuration();
}