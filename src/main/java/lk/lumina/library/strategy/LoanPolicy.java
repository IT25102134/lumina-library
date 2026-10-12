package lk.lumina.library.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Strategy for the existing loan-period and overdue-fine rules. */
public interface LoanPolicy {
    LocalDateTime dueAt(LocalDateTime issuedAt);
    BigDecimal overdueFine(LocalDateTime dueAt, LocalDateTime returnedAt);
}
