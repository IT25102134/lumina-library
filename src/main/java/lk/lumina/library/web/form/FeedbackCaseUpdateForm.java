package lk.lumina.library.web.form;

import jakarta.validation.constraints.NotNull;

import lk.lumina.library.model.FeedbackPriority;
import lk.lumina.library.model.FeedbackStatus;

/** Binds manager status and priority updates. */
public class FeedbackCaseUpdateForm {

    @NotNull(message = "Please select a status")
    private FeedbackStatus status;

    @NotNull(message = "Please select a priority")
    private FeedbackPriority priority;

    public FeedbackStatus getStatus() {
        return status;
    }

    public void setStatus(FeedbackStatus status) {
        this.status = status;
    }

    public FeedbackPriority getPriority() {
        return priority;
    }

    public void setPriority(FeedbackPriority priority) {
        this.priority = priority;
    }
}
