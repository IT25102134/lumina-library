package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventRegistrationRepository extends JpaRepository<EventRegistration,Long>{
    long countByEventId(Long eventId);
    boolean existsByEventIdAndMemberId(Long eventId,Long memberId);
    List<EventRegistration> findByEventIdOrderByRegisteredAtAsc(Long eventId);
    List<EventRegistration> findByMemberIdOrderByRegisteredAtDesc(Long memberId);
    void deleteByEventId(Long eventId);
}
