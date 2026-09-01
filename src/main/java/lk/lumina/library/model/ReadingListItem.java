package lk.lumina.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="reading_list_items",uniqueConstraints=@UniqueConstraint(columnNames={"member_id","book_id"}))
public class ReadingListItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="member_id") private UserAccount member;
    @ManyToOne(optional=false) @JoinColumn(name="book_id") private Book book;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    public ReadingListItem(){} public ReadingListItem(UserAccount m,Book b){member=m;book=b;}
    public Long getId(){return id;} public UserAccount getMember(){return member;} public Book getBook(){return book;} public LocalDateTime getCreatedAt(){return createdAt;}
}
