package lk.lumina.library.repository;

import java.util.List;

import lk.lumina.library.model.FeedbackReply;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackReplyRepository extends JpaRepository<FeedbackReply, Long> {

    List<FeedbackReply> findByFeedbackItemIdOrderByCreatedAtAsc(Long feedbackId);

    boolean existsByFeedbackItemId(Long feedbackId);

    // Fetch authors in the same query so the conversation thread does not lazy-load.
    @Query("""
        SELECT reply
        FROM FeedbackReply reply
        JOIN FETCH reply.author
        JOIN FETCH reply.feedbackItem
        WHERE reply.feedbackItem.id IN :feedbackIds
        ORDER BY reply.createdAt ASC
        """)
    List<FeedbackReply> findConversationReplies(@Param("feedbackIds") List<Long> feedbackIds);
}
