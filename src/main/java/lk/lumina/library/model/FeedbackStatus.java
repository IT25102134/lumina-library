package lk.lumina.library.model;

public enum FeedbackStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    CLOSED,
    WITHDRAWN;

    public String getDisplayName() {
        return name().replace('_', ' ');
    }

    /** Members may withdraw only while the case is still active. */
    public boolean isMemberCanWithdraw() {
        return this == OPEN || this == IN_REVIEW;
    }

    /** Closed and withdrawn cases stop the conversation thread. */
    public boolean isRepliesAllowed() {
        return this != CLOSED && this != WITHDRAWN;
    }

    public boolean isCompleted() {
        return this == RESOLVED || this == CLOSED || this == WITHDRAWN;
    }

    /**
     * Managers follow OPEN → IN_REVIEW → RESOLVED → CLOSED.
     * WITHDRAWN is reserved for members only.
     */
    public boolean canManagerTransitionTo(FeedbackStatus next) {
        if (next == null || next == WITHDRAWN) {
            return false;
        }
        // Same status is allowed so a manager can change priority only.
        if (this == next) {
            return true;
        }
        return switch (this) {
            case OPEN -> next == IN_REVIEW;
            case IN_REVIEW -> next == RESOLVED;
            case RESOLVED -> next == CLOSED || next == IN_REVIEW;
            case CLOSED, WITHDRAWN -> false;
        };
    }
}

