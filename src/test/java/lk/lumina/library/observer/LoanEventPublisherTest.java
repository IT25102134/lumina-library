package lk.lumina.library.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LoanEventPublisherTest {
    @Test void notifiesEveryRegisteredObserver() {
        AtomicInteger updates = new AtomicInteger();
        LoanEventObserver first = event -> updates.incrementAndGet();
        LoanEventObserver second = event -> updates.incrementAndGet();

        new LoanEventPublisher(List.of(first, second)).publish(null);

        assertEquals(2, updates.get());
    }
}
