package lk.lumina.library.model;

public enum FeedbackType {
    FEEDBACK,
    COMPLAINT,
    SUGGESTION;

    public String getDisplayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
