package lk.lumina.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=180) private String actor;
    @Column(nullable=false,length=80) private String action;
    @Column(nullable=false,length=80) private String entityType;
    private Long entityId; @Column(length=1000) private String details;
    @Column(nullable=false) private LocalDateTime occurredAt=LocalDateTime.now();
    public AuditLog(){} public AuditLog(String a,String action,String type,Long eid,String details){actor=a;this.action=action;entityType=type;entityId=eid;this.details=details;}
    public Long getId(){return id;} public String getActor(){return actor;} public String getAction(){return action;}
    public String getEntityType(){return entityType;} public Long getEntityId(){return entityId;} public String getDetails(){return details;} public LocalDateTime getOccurredAt(){return occurredAt;}
}
