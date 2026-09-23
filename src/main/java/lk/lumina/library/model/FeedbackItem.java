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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "feedback_items")
public class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Example: FCM-2026-A1B2C3D4
    @Column(
            name = "reference_number",
            nullable = false,
            unique = true,
            length = 30
    )
    private String referenceNumber;

    // Member who submitted the case
    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private UserAccount member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackType type = FeedbackType.FEEDBACK;

    @Column(nullable = false, length = 60)
    private String category = "GENERAL";

    @Column(nullable = false, length = 180)
    private String subject;

    @Column(nullable = false, length = 2500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackPriority priority = FeedbackPriority.MEDIUM;

    /*
     * This field belongs to the old version.
     * Keep it so earlier responses are not lost.
     */
    @Column(length = 2500)
    private String response;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private boolean archived = false;

    public FeedbackItem() {
    }

    /*
     * Runs automatically before saving a new feedback case.
     */
    @PrePersist
    public void beforeInsert() {

        if (referenceNumber == null || referenceNumber.isBlank()) {

            referenceNumber =
                    "FCM-"
                            + Year.now().getValue()
                            + "-"
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase(Locale.ROOT);
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        updatedAt = createdAt;
    }

    /*
     * Runs automatically when updating a feedback case.
     */
    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public FeedbackPriority getPriority() {
        return priority;
    }

    public void setPriority(FeedbackPriority priority) {
        this.priority = priority;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}