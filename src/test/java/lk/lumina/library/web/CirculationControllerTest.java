package lk.lumina.library.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CirculationControllerTest {
    @Test void preservesTheExistingFourteenDayAndTwentyFiveRupeeRules() {
        LocalDateTime issued = LocalDateTime.of(2026, 10, 1, 9, 0);
        assertEquals(issued.plusDays(14), CirculationController.dueAt(issued));
        assertEquals(BigDecimal.valueOf(75),
            CirculationController.overdueFine(issued, issued.plusDays(3)));
    }
}
