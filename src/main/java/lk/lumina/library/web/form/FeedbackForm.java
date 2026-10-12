package lk.lumina.library.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lk.lumina.library.model.FeedbackItem;
import lk.lumina.library.model.FeedbackType;

/** Binds member feedback submission and edit forms without exposing JPA entities. */
public class FeedbackForm {

    @NotNull(message = "Please select a case type")
    private FeedbackType type = FeedbackType.FEEDBACK;

    @NotBlank(message = "Please select a category")
    @Size(max = 60, message = "Category is too long")
    private String category;

    @NotBlank(message = "Subject is required")
    @Size(max = 180, message = "Subject must be 180 characters or fewer")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 2500, message = "Message must be between 10 and 2500 characters")
    private String message;

    public static FeedbackForm from(FeedbackItem item) {
        FeedbackForm form = new FeedbackForm();
        form.setType(item.getType());
        form.setCategory(item.getCategory());
        form.setSubject(item.getSubject());
        form.setMessage(item.getMessage());
        return form;
    }

    public FeedbackType getType() {
        return type;
    }

    public void setType(FeedbackType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public void setCategory(String category) {
        this.category = category == null ? null : category.trim();
    }

    public void setSubject(String subject) {
        this.subject = subject == null ? null : subject.trim();
    }

    public void setMessage(String message) {
        // Validate the text that will actually be stored, after trimming spaces.
        this.message = message == null ? null : message.trim();
    }

}
