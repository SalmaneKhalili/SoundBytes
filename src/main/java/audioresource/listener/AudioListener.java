package audioresource.listener;

import audioresource.dto.AudioStatus;

/**
 * Callback interface for receiving events from an {@link audioresource.core.AudioEngine}
 * or {@link audioresource.controller.PlayBackController}.
 * <p>
 * All methods are invoked on the playback thread, so implementations should be
 * lightweight and avoid blocking.
 * </p>
 *
 * @author Salmane Khalili
 */
public interface AudioListener {

    /**
     * Called periodically during playback to report the current position.
     *
     * @param currentMicros current playback time in microseconds
     * @param totalMicros    total duration in microseconds
     */
    void onProgress(long currentMicros, long totalMicros);

    /**
     * Called when the engine's status changes (e.g., PLAYING → PAUSED).
     *
     * @param status the new status
     */
    void onStatusChanged(AudioStatus status);

    /**
     * Called when an unrecoverable error occurs during playback.
     *
     * @param e the exception that caused the error
     */
    void onError(Exception e);
}