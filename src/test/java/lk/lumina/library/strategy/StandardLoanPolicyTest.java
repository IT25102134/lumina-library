package lk.lumina.library.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StandardLoanPolicyTest {
    private final StandardLoanPolicy policy = new StandardLoanPolicy();

    @Test void preservesTheExistingFourteenDayAndTwentyFiveRupeeRules() {
        LocalDateTime issued = LocalDateTime.of(2026, 10, 1, 9, 0);
        assertEquals(issued.plusDays(14), policy.dueAt(issued));
        assertEquals(BigDecimal.valueOf(75), policy.overdueFine(issued, issued.plusDays(3)));
    }
}
