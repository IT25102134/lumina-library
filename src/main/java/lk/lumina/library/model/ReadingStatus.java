package lk.lumina.library.model;

/** Progress state for a member's personal reading-list entry. */
public enum ReadingStatus {
    PLAN_TO_READ("Plan to read"),
    READING("Currently reading"),
    COMPLETED("Completed");

    private final String displayName;

    ReadingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
