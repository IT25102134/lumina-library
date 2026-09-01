package lk.lumina.library.repository;
import java.util.*;
import java.time.LocalDateTime;
import lk.lumina.library.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<Notification,Long>{
    List<Notification> findTop8ByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    long countByRecipientIdAndReadFalse(Long recipientId);
    boolean existsByRecipientIdAndTitleAndCreatedAtAfter(Long recipientId,String title,LocalDateTime after);
}
