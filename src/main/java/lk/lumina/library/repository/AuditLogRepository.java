package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long>{
    List<AuditLog> findTop100ByOrderByOccurredAtDesc();
}
