package lk.lumina.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_replies")
public class FeedbackReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reply for Feedback/Complaint case
    @ManyToOne(optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private FeedbackItem feedbackItem;

    // Member or manager who wrote this reply.
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(nullable = false, length = 2500)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FeedbackReply() {
    }

    public FeedbackReply(
            FeedbackItem feedbackItem,
            UserAccount author,
            String message) {

        this.feedbackItem = feedbackItem;
        this.author = author;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public FeedbackItem getFeedbackItem() {
        return feedbackItem;
    }

    public void setFeedbackItem(FeedbackItem feedbackItem) {
        this.feedbackItem = feedbackItem;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public void setAuthor(UserAccount author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
