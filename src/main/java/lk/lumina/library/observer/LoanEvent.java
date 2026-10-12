package lk.lumina.library.observer;

import lk.lumina.library.model.Loan;
import lk.lumina.library.model.NotificationType;

/** Event published after an existing circulation action changes a loan. */
public record LoanEvent(Loan loan, String actor, String auditAction,
                        String notificationTitle, String notificationMessage,
                        NotificationType notificationType) {}
