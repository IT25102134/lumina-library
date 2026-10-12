package lk.lumina.library.repository;

import java.util.*;
import lk.lumina.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findAllByOrderByBarcodeAsc();
    List<BookCopy> findByBookId(Long bookId);
    Optional<BookCopy> findFirstByBookIdAndStatus(Long bookId, BookCopyStatus status);
    long countByStatus(BookCopyStatus status);
    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);
    List<BookCopy> findByStatusOrderByBarcodeAsc(BookCopyStatus status);

    @Query("select c from BookCopy c where " +
           "lower(c.barcode) like lower(concat('%', :q, '%')) or " +
           "lower(c.book.title) like lower(concat('%', :q, '%')) or " +
           "lower(c.book.isbn) like lower(concat('%', :q, '%')) or " +
           "lower(c.book.author) like lower(concat('%', :q, '%')) or " +
           "lower(c.shelfLocation) like lower(concat('%', :q, '%')) " +
           "order by c.barcode asc")
    List<BookCopy> searchByQuery(@Param("q") String q);

    @Query("select c from BookCopy c where c.status = :status and (" +
           "lower(c.barcode) like lower(concat('%', :q, '%')) or " +
           "lower(c.book.title) like lower(concat('%', :q, '%')) or " +
           "lower(c.book.isbn) like lower(concat('%', :q, '%')) or " +
           "lower(c.book.author) like lower(concat('%', :q, '%')) or " +
           "lower(c.shelfLocation) like lower(concat('%', :q, '%'))) " +
           "order by c.barcode asc")
    List<BookCopy> searchByQueryAndStatus(@Param("q") String q, @Param("status") BookCopyStatus status);

    @Query("select count(l) from Loan l where l.copy.id = :copyId")
    long countLoansByCopyId(@Param("copyId") Long copyId);
}


