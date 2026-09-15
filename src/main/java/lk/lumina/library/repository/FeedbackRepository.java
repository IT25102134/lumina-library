package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackItem,Long>{
     /* Get all feedback cases.
      Newest cases are shown first.
     */
    List<FeedbackItem> findAllByOrderByCreatedAtDesc();
    /**
     * Get feedback cases submitted by one particular member.
     */
    List<FeedbackItem> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    long countByStatusIn(Collection<FeedbackStatus> statuses);
}
