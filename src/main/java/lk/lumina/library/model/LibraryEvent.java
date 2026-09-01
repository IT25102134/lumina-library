package lk.lumina.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity @Table(name="library_events")
public class LibraryEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable=false,length=180) private String title;
    @Column(nullable=false,length=2000) private String description;
    @Column(nullable=false,length=180) private String location;
    @Column(nullable=false) private LocalDateTime startAt;
    @Column(nullable=false) private int capacity=30;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EventStatus status=EventStatus.PUBLISHED;
    public LibraryEvent(){}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getLocation(){return location;} public void setLocation(String v){location=v;}
    public LocalDateTime getStartAt(){return startAt;} public void setStartAt(LocalDateTime v){startAt=v;}
    public int getCapacity(){return capacity;} public void setCapacity(int v){capacity=v;}
    public EventStatus getStatus(){return status;} public void setStatus(EventStatus v){status=v;}
}
