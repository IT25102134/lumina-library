package lk.lumina.library.observer;

import lk.lumina.library.service.AuditService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Records the same audit entry that circulation previously created directly. */
@Component @Order(2)
public class LoanAuditObserver implements LoanEventObserver {
    private final AuditService audit;

    public LoanAuditObserver(AuditService audit) {
        this.audit = audit;
    }

    @Override public void onLoanEvent(LoanEvent event) {
        audit.record(event.actor(), event.auditAction(), "Loan", event.loan().getId(),
            event.loan().getCopy().getBook().getTitle() + " / " + event.loan().getMember().getFullName());
    }
}
