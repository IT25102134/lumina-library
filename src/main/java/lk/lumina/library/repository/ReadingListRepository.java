package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.ReadingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReadingListRepository extends JpaRepository<ReadingListItem,Long>{
    List<ReadingListItem> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<ReadingListItem> findByMemberIdAndBookId(Long memberId,Long bookId);
}
