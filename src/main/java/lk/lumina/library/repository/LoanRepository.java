package lk.lumina.library.repository;
import java.time.LocalDateTime;
import java.util.*;
import lk.lumina.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LoanRepository extends JpaRepository<Loan,Long>{
    List<Loan> findAllByOrderByRequestedAtDesc();
    List<Loan> findByMemberIdOrderByRequestedAtDesc(Long memberId);
    List<Loan> findByStatusOrderByRequestedAtAsc(LoanStatus status);
    long countByStatus(LoanStatus status);
    long countByStatusAndDueAtBefore(LoanStatus status,LocalDateTime time);
    boolean existsByMemberIdAndCopyBookIdAndStatusIn(Long memberId,Long bookId,Collection<LoanStatus> statuses);
}
