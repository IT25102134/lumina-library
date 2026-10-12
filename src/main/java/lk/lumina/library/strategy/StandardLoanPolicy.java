package lk.lumina.library.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** Current Lumina policy: 14 days and LKR 25 for each overdue day. */
@Component
public class StandardLoanPolicy implements LoanPolicy {
    @Override public LocalDateTime dueAt(LocalDateTime issuedAt) {
        return issuedAt.plusDays(14);
    }

    @Override public BigDecimal overdueFine(LocalDateTime dueAt, LocalDateTime returnedAt) {
        if (dueAt == null) return BigDecimal.ZERO;
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueAt.toLocalDate(), returnedAt.toLocalDate()));
        return BigDecimal.valueOf(overdueDays * 25L);
    }
}
