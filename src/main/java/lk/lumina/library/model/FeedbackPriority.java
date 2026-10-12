package lk.lumina.library.model;

public enum FeedbackPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    public String getDisplayName() {
        return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
    }
}
