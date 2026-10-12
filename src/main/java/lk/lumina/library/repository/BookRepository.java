package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.Book;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
// Provides persistence and catalogue-specific queries for Book records.
public interface BookRepository extends JpaRepository<Book,Long>{
    // Searches active titles by title, author, ISBN, or category and keeps results alphabetically ordered.
    @Query("select b from Book b where b.active=true and (lower(b.title) like lower(concat('%',:q,'%')) or lower(b.author) like lower(concat('%',:q,'%')) or lower(b.isbn) like lower(concat('%',:q,'%')) or lower(b.category) like lower(concat('%',:q,'%'))) order by b.title")
    List<Book> search(@Param("q") String q);
    // Supplies the default catalogue listing while excluding soft-deleted titles.
    List<Book> findByActiveTrueOrderByTitleAsc();
    // Supports ISBN uniqueness checks for updates by ignoring the record currently being edited.
    boolean existsByIsbnAndIdNot(String isbn,Long id);
}
