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

    List<FeedbackItem> findByMemberIdAndArchivedFalseOrderByCreatedAtDesc(Long memberId);

    Optional<FeedbackItem> findByIdAndMemberId(Long id, Long memberId);

    Optional<FeedbackItem> findByIdAndMemberIdAndArchivedFalse(Long id, Long memberId);

    Optional<FeedbackItem> findByIdAndArchivedFalse(Long id);

    long countByStatusIn(Collection<FeedbackStatus> statuses);

    long countByArchivedFalse();

    long countByStatusAndArchivedFalse(FeedbackStatus status);

    long countByTypeAndArchivedFalse(FeedbackType type);

    // Optional filters stay null so one query can search the active manager queue.
    // JOIN FETCH loads the member in the same query for the manager case list.
    @Query("""
        SELECT item
        FROM FeedbackItem item
        JOIN FETCH item.member member
        WHERE item.archived = false
          AND (:type IS NULL OR item.type = :type)
          AND (:status IS NULL OR item.status = :status)
          AND (:priority IS NULL OR item.priority = :priority)
          AND (
                :keyword IS NULL
                OR LOWER(item.referenceNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(item.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(item.message) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(member.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(member.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(member.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(member.membershipNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        ORDER BY item.createdAt DESC
        """)
    List<FeedbackItem> searchForManager(
            @Param("type") FeedbackType type,
            @Param("status") FeedbackStatus status,
            @Param("priority") FeedbackPriority priority,
            @Param("keyword") String keyword);
}
