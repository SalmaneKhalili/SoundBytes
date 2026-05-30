package audioresource.controller;

import audioresource.core.AudioOutput;
import audioresource.decoder.Decoder;
import audioresource.dto.AudioStatus;
import audioresource.listener.AudioListener;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates the audio playback loop, connecting a {@link Decoder},
 *  and an {@link AudioOutput}.
 * <p>
 * Runs a dedicated worker thread that reads PCM data from the decoder,
 * fills the ring buffer, writes to the audio output, and reports progress
 * and latency statistics (P50, P95). Supports play, pause, resume, stop, and seek.
 * </p>
 *
 * @author Salmane Khalili
 */
public class PlayBackController {
    private static final int TRANSFER_SIZE = 4096;
    private static final int WINDOW_SIZE = 1000;
    private final long[] latencies = new long[WINDOW_SIZE];
    private int latencyIdx = 0;
    private int readCount = 0;
    private volatile long lastP50 = 0;
    private volatile long lastP95 = 0;
    private volatile boolean producerDone = false;


    private final List<AudioListener> listeners = new CopyOnWriteArrayList<>();
    private final Decoder decoder;
    private ExecutorService executor;
    private final ArrayBlockingQueue<byte[]> queue;
    private final AudioOutput audioOutput;
    private final AtomicReference<AudioStatus> status = new AtomicReference<>(AudioStatus.STOPPED);
    /**
     * Constructs a new playback controller.
     *
     * @param decoder     the audio decoder
     * @param queue  the queue for PCM data
     * @param audioOutput the audio output device
     */
    public PlayBackController(Decoder decoder, ArrayBlockingQueue<byte[]> queue , AudioOutput audioOutput) {
        this.decoder = decoder;
        this.queue = queue;
        this.audioOutput = audioOutput;
    }

    /** Notifies all listeners about a status change. */
    private void broadcastStatus(AudioStatus status) {
        for (AudioListener l : listeners) {
            l.onStatusChanged(status);
        }
    }

    public AudioStatus getStatus(){
        return status.get();
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
    private void produce(){
        byte[] transfer = new byte[TRANSFER_SIZE];
        long lastUpdate = 0;
        while (status.get() == AudioStatus.PLAYING || status.get() == AudioStatus.PAUSED){
            long start = System.nanoTime();
            int n = decoder.read(ByteBuffer.wrap(transfer));
            long end = System.nanoTime();
            if (n == -1){
                producerDone = true;
                broadcastStatus(AudioStatus.FINISHED);
                return;
            }
            recordLatency((end - start) / 1000);
            try {
                queue.put(Arrays.copyOf(transfer, n));
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               return;
            }
            long now = System.currentTimeMillis();
            lastUpdate = getLastUpdate(now, lastUpdate);
        }
    }

    private void consume() {
        audioOutput.play();
        try {
            while (status.get() != AudioStatus.STOPPED && (!producerDone || !queue.isEmpty())) {
                if (status.get() == AudioStatus.PAUSED) {
                    Thread.sleep(100);
                    continue;
                }
                byte[] chunk = queue.poll(10, TimeUnit.MILLISECONDS);
                if (chunk == null) continue;
                audioOutput.write(chunk, 0, chunk.length);
            }
        } catch (InterruptedException e) {
           Thread.currentThread().interrupt();
        } finally {
            audioOutput.close();
        }
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
        int cap = queue.remainingCapacity() + queue.size();
        return (cap == 0)? 0 : (queue.size() * 100) / cap;
    }

    /**
     * Starts or resumes playback. Stops any previous worker thread and launches a new one.
     * The worker thread reads PCM data and writes to the audio output until stopped or EOF.
     */
    public void play() {
        if (!status.compareAndSet(AudioStatus.STOPPED, AudioStatus.PLAYING)){return;}
        executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.submit(this::produce);
        executor.submit(this::consume);
    }

    /** Pauses playback. The worker thread remains alive but will not consume more data. */
    public void pause() {
        status.set(AudioStatus.PAUSED);
        audioOutput.pause();
        broadcastStatus(AudioStatus.PAUSED);
    }

    /** Resumes playback after a pause. */
    public void resume() {
        status.set(AudioStatus.PLAYING);
        audioOutput.play();
    }

    /** Stops playback permanently and allows the worker thread to terminate. */
    public void stop() {
        status.set(AudioStatus.STOPPED);
        if (executor != null) {
            executor.shutdownNow();
            try { executor.awaitTermination(1, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) {}
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
            decoder.seek(microseconds);
            queue.clear();
            if (audioOutput != null) audioOutput.flush();
    }

    /**
     * Blocks the calling thread until playback finishes (natural end or {@link #stop()}).
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void waitUntilFinished() throws InterruptedException {
        if (executor != null) executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    /** Stops playback, waits for the worker thread to finish, and closes the audio output. */
    public void close() {
        stop();
        if (executor != null) {
            try {
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        if (audioOutput != null) audioOutput.close();
    }
}