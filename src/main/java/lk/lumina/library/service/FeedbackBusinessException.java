package lk.lumina.library.service;

/**
 * Represents an expected failure of a feedback business rule.
 * The controller displays its message to the user.
 */
public class FeedbackBusinessException extends RuntimeException {

    public FeedbackBusinessException(String message) {
        super(message);
    }
}
