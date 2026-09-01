package lk.lumina.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="notifications")
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="recipient_id") private UserAccount recipient;
    @Column(nullable=false,length=180) private String title;
    @Column(nullable=false,length=1000) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private NotificationType type=NotificationType.INFO;
    @Column(name="is_read",nullable=false) private boolean read=false;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    public Notification(){} public Notification(UserAccount r,String t,String m,NotificationType type){recipient=r;title=t;message=m;this.type=type;}
    public Long getId(){return id;} public UserAccount getRecipient(){return recipient;} public void setRecipient(UserAccount v){recipient=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public NotificationType getType(){return type;} public void setType(NotificationType v){type=v;} public boolean isRead(){return read;} public void setRead(boolean v){read=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
