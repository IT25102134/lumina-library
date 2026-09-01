package lk.lumina.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, unique = true, length = 20) private String isbn;
    @NotBlank @Column(nullable = false, length = 220) private String title;
    @NotBlank @Column(nullable = false, length = 160) private String author;
    @Column(nullable = false, length = 100) private String category;
    @Column(length = 160) private String publisher;
    private Integer publicationYear;
    @Column(length = 2000) private String description;
    @Column(length = 500) private String coverUrl;
    @Column(length = 500) private String ebookUrl;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Book() {}
    public Book(String isbn, String title, String author, String category) {
        this.isbn = isbn; this.title = title; this.author = author; this.category = category;
    }
    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String v) { isbn = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getAuthor() { return author; }
    public void setAuthor(String v) { author = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String v) { publisher = v; }
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer v) { publicationYear = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String v) { coverUrl = v; }
    public String getEbookUrl() { return ebookUrl; }
    public void setEbookUrl(String v) { ebookUrl = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
