package lk.lumina.library.service;

import lk.lumina.library.model.EventStatus;

public final class EventStatusManager {

    // Stores the single instance
    private static EventStatusManager instance;

    // Private constructor
    private EventStatusManager() {
    }

    // Returns the same instance
    public static synchronized EventStatusManager getInstance() {
        if (instance == null) {
            instance = new EventStatusManager();
        }
        return instance;
    }

    // Existing event status defaulting behavior
    public EventStatus resolveStatus(EventStatus status) {
        return status == null
                ? EventStatus.PUBLISHED
                : status;
    }
}