package lk.lumina.library.model;

public enum Role {
    HEAD_LIBRARIAN("Head Librarian"),
    CIRCULATION_STAFF("Circulation Desk Staff"),
    MEMBER("Library Member"),
    LIBRARY_ASSISTANT("Library Assistant"),
    EVENT_COORDINATOR("Event Coordinator"),
    LIBRARY_MANAGER("Library Manager");

    private final String displayName;
    Role(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
