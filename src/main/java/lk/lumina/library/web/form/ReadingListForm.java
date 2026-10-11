package lk.lumina.library.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.lumina.library.model.ReadingPriority;
import lk.lumina.library.model.ReadingStatus;

/** Validated browser input for create and update reading-list operations. */
public class ReadingListForm {
    private Long bookId;

    @NotNull(message = "Choose a reading status.")
    private ReadingStatus status = ReadingStatus.PLAN_TO_READ;

    @NotNull(message = "Choose a priority.")
    private ReadingPriority priority = ReadingPriority.NORMAL;

    @Size(max = 500, message = "Notes must be 500 characters or fewer.")
    private String notes;

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public ReadingStatus getStatus() { return status; }
    public void setStatus(ReadingStatus status) { this.status = status; }
    public ReadingPriority getPriority() { return priority; }
    public void setPriority(ReadingPriority priority) { this.priority = priority; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
