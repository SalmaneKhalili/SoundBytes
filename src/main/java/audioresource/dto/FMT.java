package audioresource.dto;
/**
 * Immutable data carrier for the {@code fmt } chunk of a WAV file.
 * <p>
 * Contains all parameters necessary to interpret the PCM audio stream:
 * sample rate, channel count, bit depth, byte rate, block alignment, and audio format code.
 * </p>
 *
 * @param chunkSize     size of the fmt chunk (usually 16 for PCM)
 * @param audioFormat   format code (1 = PCM, other values indicate compression)
 * @param numChannels   number of audio channels (1 = mono, 2 = stereo)
 * @param sampleRate    samples per second per channel
 * @param byteRate      (sampleRate * numChannels * bitsPerSample/8)
 * @param blockAlign    (numChannels * bitsPerSample/8) – frame size in bytes
 * @param bitsPerSample bits per sample (8, 16, 24, etc.)
 */

public record FMT(int chunkSize,
                  int audioFormat,
                  int numChannels,
                  int sampleRate,
                  int byteRate,
                  int blockAlign,
                  int bitsPerSample
                  ) {
}
