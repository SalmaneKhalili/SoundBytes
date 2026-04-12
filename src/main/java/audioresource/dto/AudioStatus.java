package audioresource.dto;

/**
 * Represents the possible states of an {@link audioresource.core.AudioEngine}
 * or {@link audioresource.controller.PlayBackController}.
 *
 * @author Salmane Khalili
 */
public enum AudioStatus {
    PLAYING,
    PAUSED,
    STOPPED,
    FINISHED
}