package lk.lumina.library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Represents a saved catalogue entry in a member's personal reading list.
 *  Unique per (member, book). Status, priority and private notes are optional
 *  on creation (defaults applied) and editable via the reading-list CRUD. */
@Entity
@Table(name = "reading_list_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "book_id"}))
public class ReadingListItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "member_id") private UserAccount member;
    @ManyToOne(optional = false) @JoinColumn(name = "book_id")   private Book book;

    /** Reading progress status (defaults to PLAN_TO_READ). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReadingStatus status = ReadingStatus.PLAN_TO_READ;

    /** Member-assigned priority (defaults to NORMAL). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingPriority priority = ReadingPriority.NORMAL;

    /** Private reading notes, max 500 characters. */
    @Column(length = 500)
    private String notes;

    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public ReadingListItem() {}
    public ReadingListItem(UserAccount member, Book book) {
        this.member = member; this.book = book;
    }

    public Long getId()                  { return id; }
    public UserAccount getMember()       { return member; }
    public Book getBook()                { return book; }
    public ReadingStatus getStatus()     { return status; }
    public void setStatus(ReadingStatus v)    { status = v; }
    public ReadingPriority getPriority() { return priority; }
    public void setPriority(ReadingPriority v){ priority = v; }
    public String getNotes()             { return notes; }
    public void setNotes(String v)       { notes = v; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
