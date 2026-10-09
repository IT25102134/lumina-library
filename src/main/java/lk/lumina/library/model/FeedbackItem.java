package lk.lumina.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="feedback_items")
public class FeedbackItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="member_id") private UserAccount member;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private FeedbackType type=FeedbackType.FEEDBACK;
    @Column(nullable=false,length=180) private String subject;
    @Column(nullable=false,length=2500) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private FeedbackStatus status=FeedbackStatus.OPEN;
    @Column(length=2500) private String response;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    private LocalDateTime resolvedAt;
    public FeedbackItem(){}


    public Long getId(){return id;} public UserAccount getMember(){return member;} public void setMember(UserAccount v){member=v;}
    public FeedbackType getType(){return type;} public void setType(FeedbackType v){type=v;}
    public String getSubject(){return subject;} public void setSubject(String v){subject=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public FeedbackStatus getStatus(){return status;} public void setStatus(FeedbackStatus v){status=v;}
    public String getResponse(){return response;} public void setResponse(String v){response=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getResolvedAt(){return resolvedAt;} public void setResolvedAt(LocalDateTime v){resolvedAt=v;}
}
