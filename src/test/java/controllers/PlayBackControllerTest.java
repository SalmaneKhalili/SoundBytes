package controllers;
import audioresource.controller.PlayBackController;
import audioresource.core.AudioOutput;
import audioresource.decoder.Decoder;
import audioresource.dto.AudioStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
public class PlayBackControllerTest {
    private static final Decoder emptyDecoder = new Decoder() {
        @Override public int read(ByteBuffer buffer) { return -1; }
        @Override public long seek(long micros) { return micros; }
        @Override public long getCurrentTimeMicros() { return 0; }
        @Override public long getAudioDuration() { return 0; }
        @Override public AudioFormat getAudioFormat() {
            return new AudioFormat(44100, 16, 2, true, false);
        }
    };
    private static final AudioOutput silentOutput = new AudioOutput() {
        @Override public int write(byte[] b, int o, int l) { return l; }
        @Override public void play() {}
        @Override public void pause() {}
        @Override public void drain() {}
        @Override public void flush() {}
        @Override public void close() {}
    };
    private PlayBackController ctrl;

    @BeforeEach
    void setUp(){
        ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(16);
        ctrl = new PlayBackController(emptyDecoder, queue,silentOutput);
    }

    @Test
    void playTransitionsToPlaying() {
        assertEquals(AudioStatus.STOPPED, ctrl.getStatus());
        ctrl.play();
        assertEquals(AudioStatus.PLAYING, ctrl.getStatus());
        ctrl.stop();
    }

    @Test
    void pauseTransitionsToPaused(){
        ctrl.play();
        ctrl.pause();
        assertEquals(AudioStatus.PAUSED, ctrl.getStatus());
        ctrl.stop();
    }

    @Test
    void stopTransitionsToStopped(){
        ctrl.play();
        ctrl.stop();
        assertEquals(AudioStatus.STOPPED, ctrl.getStatus());
    }
}
