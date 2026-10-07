package lk.lumina.library.observer;

import java.util.List;
import org.springframework.stereotype.Component;

/** Subject in the Observer pattern; Spring supplies all registered observers. */
@Component
public class LoanEventPublisher {
    private final List<LoanEventObserver> observers;

    public LoanEventPublisher(List<LoanEventObserver> observers) {
        this.observers = observers;
    }

    public void publish(LoanEvent event) {
        observers.forEach(observer -> observer.onLoanEvent(event));
    }
}
