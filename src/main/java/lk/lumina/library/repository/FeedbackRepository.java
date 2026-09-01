package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FeedbackRepository extends JpaRepository<FeedbackItem,Long>{
    List<FeedbackItem> findAllByOrderByCreatedAtDesc();
    List<FeedbackItem> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    long countByStatusIn(Collection<FeedbackStatus> statuses);
}
