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
    FINISHED;
    public boolean canTransitionTo(AudioStatus next) {
        return switch (this){
            case STOPPED -> next == PLAYING;
            case PLAYING -> next == PAUSED || next == STOPPED || next == FINISHED;
            case PAUSED -> next == PLAYING || next == STOPPED;
            case FINISHED -> next == STOPPED;
        };
    }
}