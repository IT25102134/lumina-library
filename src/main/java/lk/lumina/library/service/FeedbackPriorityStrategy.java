package lk.lumina.library.service;

import lk.lumina.library.model.FeedbackPriority;
import lk.lumina.library.model.FeedbackType;

// Strategy Pattern for automatic feedback priority calculation
public enum FeedbackPriorityStrategy {

    COMPLAINT {
        @Override
        public FeedbackPriority calculate(String category) {
            if ("Facilities".equalsIgnoreCase(category)
                    || "Staff Support".equalsIgnoreCase(category)) {
                return FeedbackPriority.HIGH;
            }
            return FeedbackPriority.MEDIUM;
        }
    },

    FEEDBACK {
        @Override
        public FeedbackPriority calculate(String category) {
            return FeedbackPriority.LOW;
        }
    },

    SUGGESTION {
        @Override
        public FeedbackPriority calculate(String category) {
            return FeedbackPriority.LOW;
        }
    };

    public abstract FeedbackPriority calculate(String category);

    public static FeedbackPriority calculatePriority(
            FeedbackType type, String category) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "Feedback type is required"
            );
        }

        return valueOf(type.name()).calculate(category);
    }
}
