package lk.lumina.library.service;

/**
 * Read-only totals displayed on the manager page.
 * This is not a database entity.
 */
public record FeedbackStats(
        long activeCases,
        long openCases,
        long inReviewCases,
        long resolvedCases,
        long complaintCases) {
}
