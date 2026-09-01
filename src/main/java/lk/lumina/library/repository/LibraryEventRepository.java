package lk.lumina.library.repository;
import java.time.LocalDateTime;
import java.util.*;
import lk.lumina.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LibraryEventRepository extends JpaRepository<LibraryEvent,Long>{
    List<LibraryEvent> findByStatusAndStartAtAfterOrderByStartAtAsc(EventStatus status,LocalDateTime now);
    List<LibraryEvent> findAllByOrderByStartAtDesc();
}
