package lk.lumina.library.model;

/** Priority selected by a member for a personal reading-list entry. */
public enum ReadingPriority {
    HIGH("High priority"),
    NORMAL("Normal priority"),
    LOW("Low priority");

    private final String displayName;

    ReadingPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
