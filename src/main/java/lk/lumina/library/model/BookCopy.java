package lk.lumina.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity @Table(name = "book_copies")
public class BookCopy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "book_id") private Book book;
    @NotBlank @Column(nullable = false, unique = true, length = 50) private String barcode;
    @Column(nullable = false, length = 60) private String shelfLocation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookCopyStatus status = BookCopyStatus.AVAILABLE;
    private LocalDate acquiredDate = LocalDate.now();
    public BookCopy() {}
    public BookCopy(Book book, String barcode, String shelfLocation) { this.book=book; this.barcode=barcode; this.shelfLocation=shelfLocation; }
    public Long getId(){return id;} public Book getBook(){return book;} public void setBook(Book v){book=v;}
    public String getBarcode(){return barcode;} public void setBarcode(String v){barcode=v;}
    public String getShelfLocation(){return shelfLocation;} public void setShelfLocation(String v){shelfLocation=v;}
    public BookCopyStatus getStatus(){return status;} public void setStatus(BookCopyStatus v){status=v;}
    public LocalDate getAcquiredDate(){return acquiredDate;} public void setAcquiredDate(LocalDate v){acquiredDate=v;}
}
