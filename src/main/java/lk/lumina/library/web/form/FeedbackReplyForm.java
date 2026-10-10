package lk.lumina.library.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Binds member and manager reply forms. */
public class FeedbackReplyForm {

    @NotBlank(message = "Reply message is required")
    @Size(min = 5, max = 2500, message = "Reply must be between 5 and 2500 characters")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "Reply must include a letter")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        // Keep form validation consistent with FeedbackService's reply rule.
        this.message = message == null ? null : message.trim();
    }
}
