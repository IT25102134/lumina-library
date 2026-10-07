package lk.lumina.library.observer;

/** Observer contract for independent reactions to a circulation event. */
public interface LoanEventObserver {
    void onLoanEvent(LoanEvent event);
}
