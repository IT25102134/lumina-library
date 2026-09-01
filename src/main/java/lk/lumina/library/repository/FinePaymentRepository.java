package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FinePaymentRepository extends JpaRepository<FinePayment,Long>{
    List<FinePayment> findByMemberIdOrderByIdDesc(Long memberId);
    long countByStatus(PaymentStatus status);
}
