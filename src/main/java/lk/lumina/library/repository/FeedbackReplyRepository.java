package lk.lumina.library.repository;

import java.util.List;

import lk.lumina.library.model.FeedbackReply;

import org.springframework.data.jpa.repository.JpaRepository;

/*
 * Feedback replies database table
 * Related Repository interface
 *
 * JpaRepository used save(), findById(),
 * findAll(), delete()  methods Automatically created
 */
public interface FeedbackReplyRepository
        extends JpaRepository<FeedbackReply, Long> {

    List<FeedbackReply>
    findByFeedbackItemIdOrderByCreatedAtAsc(Long feedbackId);

    // Checking the feedback case reply

    boolean existsByFeedbackItemId(Long feedbackId);
}