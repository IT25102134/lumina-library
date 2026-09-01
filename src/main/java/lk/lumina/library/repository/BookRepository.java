package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.Book;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface BookRepository extends JpaRepository<Book,Long>{
    @Query("select b from Book b where b.active=true and (lower(b.title) like lower(concat('%',:q,'%')) or lower(b.author) like lower(concat('%',:q,'%')) or lower(b.isbn) like lower(concat('%',:q,'%')) or lower(b.category) like lower(concat('%',:q,'%'))) order by b.title")
    List<Book> search(@Param("q") String q);
    List<Book> findByActiveTrueOrderByTitleAsc();
    boolean existsByIsbnAndIdNot(String isbn,Long id);
}
