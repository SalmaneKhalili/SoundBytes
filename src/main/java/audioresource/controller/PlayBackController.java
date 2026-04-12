package audioresource.controller;

import audioresource.buffer.RingBuffer;
import audioresource.core.AudioOutput;
import audioresource.decoder.Decoder;
import audioresource.dto.AudioStatus;
import audioresource.listener.AudioListener;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

/**
 * Orchestrates the audio playback loop, connecting a {@link Decoder},
 * a {@link RingBuffer}, and an {@link AudioOutput}.
 * <p>
 * Runs a dedicated worker thread that reads PCM data from the decoder,
 * fills the ring buffer, writes to the audio output, and reports progress
 * and latency statistics (P50, P95). Supports play, pause, resume, stop, and seek.
 * </p>
 *
 * @author Salmane Khalili
 */
public class PlayBackController {


    private final Object lock = new Object();
    private static final int TRANSFER_SIZE = 4096;
    private static final int WINDOW_SIZE = 1000;
    private final long[] latencies = new long[WINDOW_SIZE];
    private int latencyIdx = 0;
    private int readCount = 0;
    private volatile long lastP50 = 0;
    private volatile long lastP95 = 0;


    private final List<AudioListener> listeners = new CopyOnWriteArrayList<>();
    private Thread workerThread;
    private final Decoder decoder;
    private final RingBuffer ringBuffer;
    private final AudioOutput audioOutput;
    private volatile AudioStatus status;

    /**
     * Constructs a new playback controller.
     *
     * @param decoder     the audio decoder
     * @param ringBuffer  the ring buffer for PCM data
     * @param audioOutput the audio output device
     */
    public PlayBackController(Decoder decoder, RingBuffer ringBuffer, AudioOutput audioOutput) {
        this.decoder = decoder;
        this.ringBuffer = ringBuffer;
        this.audioOutput = audioOutput;
    }

    /** Notifies all listeners about a status change. */
    private void broadcastStatus(AudioStatus status) {
        for (AudioListener l : listeners) {
            l.onStatusChanged(status);
        }
    }

    /**
     * Periodically broadcasts progress (at most every 100 ms).
     *
     * @param now        current system millis
     * @param lastUpdate previous broadcast time (millis)
     * @return updated lastUpdate time
     */
    private long getLastUpdate(long now, long lastUpdate) {
        if (now - lastUpdate > 100) {
            long current = decoder.getCurrentTimeMicros();
            long total = decoder.getAudioDuration();
            for (AudioListener l : listeners) {
                l.onProgress(current, total);
            }
            lastUpdate = now;
        }
        return lastUpdate;
    }

    /**
     * Reads a chunk of PCM from the decoder, writes it to the ring buffer,
     * and then drains as much as possible to the audio output.
     *
     * @param transfer temporary byte array used as a transfer buffer
     * @return {@code true} if the decoder reached EOF, {@code false} otherwise
     */
    private boolean consumePCM(byte[] transfer) {
        int bytesRead;
        synchronized (lock) {
            long start = System.nanoTime();
            bytesRead = decoder.read(ByteBuffer.wrap(transfer));
            long end = System.nanoTime();
            if (bytesRead == -1) {
                broadcastStatus(AudioStatus.FINISHED);
                return true;
            }
            long latencyMicros = (end - start) / 1000;
            recordLatency(latencyMicros);
            ringBuffer.write(transfer, 0, bytesRead);

            while (ringBuffer.available() > 0) {
                int chunk = Math.min(ringBuffer.available(), transfer.length);
                int s = ringBuffer.read(transfer, 0, chunk);
                audioOutput.write(transfer, 0, s);
            }
        }
        return false;
    }

    /**
     * Records a decoder‑read latency and updates rolling percentiles.
     *
     * @param latencyMicros latency in microseconds
     */
    public void recordLatency(long latencyMicros) {
        latencies[latencyIdx] = latencyMicros;
        latencyIdx = (latencyIdx + 1) % WINDOW_SIZE;
        readCount++;
        if (readCount % WINDOW_SIZE == 0) {
            long[] copy = latencies.clone();
            Arrays.sort(copy);
            long p50 = copy[WINDOW_SIZE / 2];
            long p95 = copy[(int) (WINDOW_SIZE * 0.95)];
            this.lastP50 = p50;
            this.lastP95 = p95;
            System.out.printf("Latency (µs) - P50: %d, P95: %d%n", p50, p95);
        }
    }

    /**
     * Returns the last computed 50th percentile latency (microseconds).
     *
     * @return P50 latency
     */
    public long getLastP50() {
        return lastP50;
    }

    /**
     * Returns the last computed 95th percentile latency (microseconds).
     *
     * @return P95 latency
     */
    public long getLastP95() {
        return lastP95;
    }

    /**
     * Returns the current ring buffer fill percentage (0‑100).
     *
     * @return buffer fill percent
     */
    public int getBufferFillPercent() {
        return (ringBuffer.available() * 100) / ringBuffer.getCapacity();
    }

    /**
     * Starts or resumes playback. Stops any previous worker thread and launches a new one.
     * The worker thread reads PCM data and writes to the audio output until stopped or EOF.
     */
    public void play() {
        if (workerThread != null && workerThread.isAlive()) {
            stop();
            try {
                workerThread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
        status = AudioStatus.PLAYING;
        broadcastStatus(AudioStatus.PLAYING);
        workerThread = new Thread(() -> {
            try {
                audioOutput.play();
                byte[] transfer = new byte[TRANSFER_SIZE];
                long lastUpdate = 0;
                while (status != AudioStatus.STOPPED) {
                    while (status == AudioStatus.PAUSED || status == AudioStatus.STOPPED) {
                        LockSupport.park();
                    }
                    if (status == AudioStatus.STOPPED || consumePCM(transfer)) break;
                    long now = System.currentTimeMillis();
                    lastUpdate = getLastUpdate(now, lastUpdate);
                }
                audioOutput.close();
            } catch (Exception e) {
                for (AudioListener l : listeners) {
                    l.onError(e);
                }
            }
        });
        workerThread.start();
    }

    /** Pauses playback. The worker thread remains alive but will not consume more data. */
    public void pause() {
        status = AudioStatus.PAUSED;
        if (audioOutput != null) audioOutput.pause();
        broadcastStatus(AudioStatus.PAUSED);
    }

    /** Resumes playback after a pause. */
    public void resume() {
        status = AudioStatus.PLAYING;
        if (audioOutput != null) {
            audioOutput.play();
            if (workerThread != null) {
                LockSupport.unpark(workerThread);
            }
        }
        broadcastStatus(AudioStatus.PLAYING);
    }

    /** Stops playback permanently and allows the worker thread to terminate. */
    public void stop() {
        status = AudioStatus.STOPPED;
        if (workerThread != null) {
            LockSupport.unpark(workerThread);
        }
        broadcastStatus(AudioStatus.STOPPED);
    }

    /**
     * Adds a listener to receive playback events.
     *
     * @param listener the listener to add
     */
    public void addListener(AudioListener listener) {
        listeners.add(listener);
    }

    /**
     * Seeks to a new absolute position (microseconds). Clears the ring buffer and flushes
     * the audio output to avoid playing stale data.
     *
     * @param microseconds target position from the start of audio
     */
    public void seek(long microseconds) {
        synchronized (lock) {
            decoder.seek(microseconds);
            ringBuffer.clear();
            if (audioOutput != null) audioOutput.flush();
        }
    }

    /**
     * Blocks the calling thread until playback finishes (natural end or {@link #stop()}).
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void waitUntilFinished() throws InterruptedException {
        if (workerThread != null) workerThread.join();
    }

    /** Stops playback, waits for the worker thread to finish, and closes the audio output. */
    public void close() {
        stop();
        if (workerThread != null) {
            try {
                workerThread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
        if (audioOutput != null) audioOutput.close();
    }
}