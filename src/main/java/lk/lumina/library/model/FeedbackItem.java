package lk.lumina.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Represents one feedback, complaint, or suggestion submitted by a member.
 * This class is mapped to the feedback_items table in the database.
 */
@Entity
@Table(name = "feedback_items")
public class FeedbackItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many feedback items can belong to one member.
     * member_id is stored as a foreign key in feedback_items.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id")
    private UserAccount member;

    /**
     * Type of message:
     * FEEDBACK, COMPLAINT, or SUGGESTION.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackType type = FeedbackType.FEEDBACK;


    /**
     * Short title entered by the member.
     */
    @Column(nullable = false, length = 180)
    private String subject;


    /**
     * Detailed message entered by the member.
     */
    @Column(nullable = false, length = 2500)
    private String message;


    // ------------------------------------------------------------
    // 4. CASE STATUS AND MANAGER RESPONSE
    // ------------------------------------------------------------

    /**
     * Current status of the case.
     * New cases automatically start as OPEN.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.OPEN;


    /**
     * Response written by the Library Manager.
     */
    @Column(length = 2500)
    private String response;

    /**
     * Date and time when the member submitted the case.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();


    /**
     * Date and time when the case was resolved or closed.
     */
    private LocalDateTime resolvedAt;


    // JPA requires a no-argument constructor.
    public FeedbackItem() {
    }

    // 6. GETTERS AND SETTERS

    public Long getId() {
        return id;
    }

    public UserAccount getMember() {
        return member;
    }

    public void setMember(UserAccount member) {
        this.member = member;
    }

    public FeedbackType getType() {
        return type;
    }

    public void setType(FeedbackType type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public void setStatus(FeedbackStatus status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}