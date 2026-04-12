package decoders;

import audioresource.dto.FMT;
import audioresource.decoder.WAVDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class WAVDecoderTest {
    private WAVDecoder decoder;
    private final String TEST_FILE = "/home/salmane/Downloads/file_example_WAV_1MG.wav";

    @BeforeEach
    void setUp() throws Exception {
        decoder = new WAVDecoder(TEST_FILE);
    }

    @Test
    void testInitialization_ParsesFmtCorrect() {
        FMT fmt = decoder.getFmtData();

        assertNotNull(fmt,"FMT Shouldnt be null after init");
        assertTrue(fmt.sampleRate()>0, "Sample rate should be positive");
        assertTrue(fmt.numChannels() == 1 || fmt.numChannels() == 2, "Channels should be mono or stereo");
        assertEquals(16, fmt.bitsPerSample(), "Standard test WAVs are usually 16-bit");
    }

    @Test
    void testAudioOffsters_AreWithinBounds() {
        long start = decoder.getAudioStartOffset();
        long size = decoder.getAudioDataSize();
        long totalFile = new File(TEST_FILE).length();

        assertTrue(start >= 44, "Standard dictates that data begins at or after byte 44");
        assertTrue(start + size <= totalFile, "audio data should be less than the total file size");
    }

    @Test
    void testGetAudioFormat_MappingIsCorrect() {
        AudioFormat format = decoder.getAudioFormat();

        // 1-to-1 Mapping Check
        assertEquals(44100.0f, format.getSampleRate(), "Sample rate mismatch");
        assertEquals(16, format.getSampleSizeInBits(), "Bit depth mismatch");
        assertEquals(2, format.getChannels(), "Channels mismatch");
        assertFalse(format.isBigEndian(), "WAV must be Little-Endian");
    }

    @Test
    void testGetDuration_MathConsistency() {
        long micros = decoder.getAudioDuration();
        long audioSize = decoder.getAudioDataSize();

        // If a song is 10MB at 176,400 bytes/sec, it should be ~56 seconds
        assertTrue(micros > 0, "Duration should be positive");

        // Duration * ByteRate should roughly equal AudioDataSize
        long calculatedSize = (micros * decoder.getFmtData().byteRate()) / 1_000_000L;
        assertEquals(audioSize, calculatedSize, 100, "Duration math desynced from data size");
    }

    @Test
    void testSeek_AlignmentAndBoundary() throws Exception {
        long halfWay = decoder.getAudioDuration() / 2;
        decoder.seek(halfWay);

        // Verify the needle moved to an even byte (Block Alignment)
        long currentPos = decoder.getPosition();
        int blockAlign = decoder.getFmtData().blockAlign();

        assertEquals(0, (currentPos - decoder.getAudioStartOffset()) % blockAlign,
                "Seek landed between frames! This causes static/noise.");
    }

    @Test
    void testRead_StopsAtDataBoundary() throws Exception {
        decoder.seek(decoder.getAudioDuration());
        ByteBuffer buffer = ByteBuffer.allocate(2048);

        int firstRead = decoder.read(buffer);
        int secondRead = decoder.read(buffer);

        assertEquals(-1, secondRead, "Decoder must signal EOF after audio data is exhausted");
        assertTrue(decoder.getPosition() <= (decoder.getAudioStartOffset() + decoder.getAudioDataSize()));
    }







}
