package lk.lumina.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="event_registrations",uniqueConstraints=@UniqueConstraint(columnNames={"event_id","member_id"}))
public class EventRegistration {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="event_id") private LibraryEvent event;
    @ManyToOne(optional=false) @JoinColumn(name="member_id") private UserAccount member;
    @Column(nullable=false) private LocalDateTime registeredAt=LocalDateTime.now();
    @Column(nullable=false) private boolean attended=false;
    public EventRegistration(){} public EventRegistration(LibraryEvent event,UserAccount member){this.event=event;this.member=member;}
    public Long getId(){return id;} public LibraryEvent getEvent(){return event;} public void setEvent(LibraryEvent v){event=v;}
    public UserAccount getMember(){return member;} public void setMember(UserAccount v){member=v;}
    public LocalDateTime getRegisteredAt(){return registeredAt;} public boolean isAttended(){return attended;} public void setAttended(boolean v){attended=v;}
}
