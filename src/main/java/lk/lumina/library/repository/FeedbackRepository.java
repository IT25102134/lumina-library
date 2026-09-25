package lk.lumina.library.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lk.lumina.library.model.FeedbackItem;
import lk.lumina.library.model.FeedbackPriority;
import lk.lumina.library.model.FeedbackStatus;
import lk.lumina.library.model.FeedbackType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<FeedbackItem, Long> {

    List<FeedbackItem> findAllByOrderByCreatedAtDesc();

    List<FeedbackItem> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * Scopes lookup to both item ID and member ID to enforce horizontal
     * authorization and prevent insecure direct object references (IDOR).
     */
    Optional<FeedbackItem> findByIdAndMemberId(Long id, Long memberId);

    long countByStatusIn(Collection<FeedbackStatus> statuses);

    long countByArchivedFalse();

    long countByStatusAndArchivedFalse(FeedbackStatus status);

    long countByTypeAndArchivedFalse(FeedbackType type);

    /**
     * Dynamic search for staff/manager dashboards. Supports optional filtering by
     * type, status, and priority, with case-insensitive partial keyword matching across
     * reference number, subject, and submitter name.
     */
    @Query("""
        SELECT item
        FROM FeedbackItem item
        WHERE item.archived = false
          AND (:type IS NULL OR item.type = :type)
          AND (:status IS NULL OR item.status = :status)
          AND (:priority IS NULL OR item.priority = :priority)
          AND (
                :keyword IS NULL
                OR LOWER(item.referenceNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(item.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(item.member.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(item.member.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        ORDER BY item.createdAt DESC
        """)
    List<FeedbackItem> searchForManager(
            @Param("type") FeedbackType type,
            @Param("status") FeedbackStatus status,
            @Param("priority") FeedbackPriority priority,
            @Param("keyword") String keyword
    );
}