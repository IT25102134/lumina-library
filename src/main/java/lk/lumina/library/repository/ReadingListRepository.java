package lk.lumina.library.repository;

import java.util.*;
import lk.lumina.library.model.ReadingListItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingListRepository extends JpaRepository<ReadingListItem, Long> {
    List<ReadingListItem> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<ReadingListItem> findByMemberIdAndBookId(Long memberId, Long bookId);

    /** Ownership-safe lookup used by edit and delete to prevent cross-member access. */
    Optional<ReadingListItem> findByIdAndMemberId(Long id, Long memberId);

    /** Used by catalogue page to compute the set of saved book IDs for the current member. */
    boolean existsByMemberIdAndBookId(Long memberId, Long bookId);
}
