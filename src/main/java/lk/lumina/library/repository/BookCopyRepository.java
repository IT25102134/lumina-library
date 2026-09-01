package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookCopyRepository extends JpaRepository<BookCopy,Long>{
    List<BookCopy> findAllByOrderByBarcodeAsc();
    List<BookCopy> findByBookId(Long bookId);
    Optional<BookCopy> findFirstByBookIdAndStatus(Long bookId,BookCopyStatus status);
    long countByStatus(BookCopyStatus status);
}
