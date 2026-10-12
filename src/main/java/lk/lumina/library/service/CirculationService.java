package lk.lumina.library.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lk.lumina.library.model.*;
import lk.lumina.library.observer.LoanEvent;
import lk.lumina.library.observer.LoanEventPublisher;
import lk.lumina.library.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Circulation business rules. Uses existing tables, entities and repositories. */
@Service
@Transactional
public class CirculationService {
    private static final List<LoanStatus> OPEN = List.of(LoanStatus.REQUESTED, LoanStatus.ACTIVE, LoanStatus.OVERDUE);
    private final LoanRepository loans;
    private final BookCopyRepository copies;
    private final FinePaymentRepository fines;
    private final CurrentUserService current;
    private final LoanEventPublisher events;
    private final EntityManager em;

    public CirculationService(LoanRepository loans, BookCopyRepository copies, FinePaymentRepository fines,
            CurrentUserService current, LoanEventPublisher events, EntityManager em) {
        this.loans = loans;
        this.copies = copies;
        this.fines = fines;
        this.current = current;
        this.events = events;
        this.em = em;
    }

    public Loan create(Long memberId, Long copyId) {
        UserAccount member = locked(UserAccount.class, memberId, "Member");
        BookCopy copy = locked(BookCopy.class, copyId, "Book copy");
        require(copy.getStatus() == BookCopyStatus.AVAILABLE && copy.getBook().isActive(),
                "This book copy is not available. Refresh the desk and choose another copy.");
        require(noOtherOpenLoan(copy.getId(), null), "This copy already has an open request or loan.");
        eligible(member, copy.getBook().getId(), null);
        Loan loan = new Loan(member, copy);
        activate(loan);
        publish(loan, "CREATE", "Book issued", issuedMessage(loan), NotificationType.SUCCESS);
        return loan;
    }

    public void issue(Long id) {
        Loan loan = locked(Loan.class, id, "Loan");
        require(loan.getStatus() == LoanStatus.REQUESTED, "Only pending requests can be issued.");
        UserAccount member = locked(UserAccount.class, loan.getMember().getId(), "Member");
        BookCopy copy = locked(BookCopy.class, loan.getCopy().getId(), "Book copy");
        require(copy.getStatus() == BookCopyStatus.RESERVED || copy.getStatus() == BookCopyStatus.AVAILABLE,
                "The requested copy is no longer available for issue.");
        require(copy.getBook().isActive(), "This catalogue title is inactive.");
        require(noOtherOpenLoan(copy.getId(), id), "Another open loan already uses this copy.");
        eligible(member, copy.getBook().getId(), id);
        activate(loan);
        publish(loan, "ISSUE", "Book ready", issuedMessage(loan), NotificationType.SUCCESS);
    }

    public void reject(Long id) {
        Loan loan = locked(Loan.class, id, "Loan");
        require(loan.getStatus() == LoanStatus.REQUESTED, "Only pending requests can be declined.");
        BookCopy copy = locked(BookCopy.class, loan.getCopy().getId(), "Book copy");
        loan.setStatus(LoanStatus.REJECTED);
        releasePendingCopy(copy, id);
        loans.save(loan);
        publish(loan, "REJECT", "Request update", "Your request for '" + copy.getBook().getTitle()
                + "' could not be approved.", NotificationType.WARNING);
    }

    public BigDecimal returned(Long id) {
        Loan loan = locked(Loan.class, id, "Loan");
        require(isIssued(loan), "Only active or overdue loans can be returned. This loan may already be returned.");
        BookCopy copy = locked(BookCopy.class, loan.getCopy().getId(), "Book copy");
        require(noOtherOpenLoan(copy.getId(), id), "Another open loan uses this copy. Resolve it before return.");
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = overdueFine(loan.getDueAt(), now);
        if (amount.signum() > 0) {
            require(!hasFine(loan), "This loan already has a fine record. Check it before returning.");
        }
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(now);
        // Preserve DAMAGED/LOST flags set by Inventory rather than resetting them to AVAILABLE.
        if (copy.getStatus() == BookCopyStatus.LOANED) {
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copies.save(copy);
        }
        loans.save(loan);
        if (amount.signum() > 0) {
            FinePayment fine = new FinePayment();
            fine.setLoan(loan);
            fine.setMember(loan.getMember());
            fine.setAmount(amount);
            fines.save(fine);
            publish(loan, "RETURN", "Overdue fine created", "A fine of LKR " + amount
                    + " was added for the returned book.", NotificationType.PAYMENT);
        } else {
            publish(loan, "RETURN", null, null, null);
        }
        return amount;
    }

    @Transactional(readOnly = true)
    public Loan editable(Long id) {
        Loan loan = loans.findById(id).orElseThrow(() -> new IllegalArgumentException("Loan was not found. Refresh the desk."));
        require(isIssued(loan), "Only active or overdue loans can be edited.");
        return loan;
    }

    public void updateDueDate(Long id, LocalDateTime dueAt) {
        Loan loan = locked(Loan.class, id, "Loan");
        require(isIssued(loan), "Only active or overdue loans can be edited.");
        require(dueAt != null && loan.getIssuedAt() != null && dueAt.isAfter(loan.getIssuedAt()),
                "Due date must be after the issue date and time.");
        loan.setDueAt(dueAt);
        loan.setStatus(dueAt.isBefore(LocalDateTime.now()) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE);
        loans.save(loan);
        publish(loan, "UPDATE", "Loan due date updated", "The due date for '" + loan.getCopy().getBook().getTitle()
                + "' is now " + dueAt.toLocalDate() + ".", NotificationType.INFO);
    }

    public void renew(Long id) {
        Loan loan = locked(Loan.class, id, "Loan");
        require(loan.getStatus() == LoanStatus.ACTIVE && loan.getDueAt() != null
                && !loan.getDueAt().isBefore(LocalDateTime.now()), "Only active loans that are not overdue can be renewed.");
        require(loan.getRenewalCount() < 1, "This loan has already used its one renewal.");
        require(loan.getMember().isActive(), "Inactive members cannot renew loans.");
        require(!hasUnpaidFines(loan.getMember().getId()), "Clear unpaid fines before renewing.");
        loan.setDueAt(loan.getDueAt().plusDays(7));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loans.save(loan);
        publish(loan, "RENEW", "Loan renewed", "Your book is now due on " + loan.getDueAt().toLocalDate()
                + ".", NotificationType.SUCCESS);
    }

    public void delete(Long id) {
        Loan loan = locked(Loan.class, id, "Loan");
        require(loan.getStatus() == LoanStatus.REQUESTED || loan.getStatus() == LoanStatus.RETURNED
                || loan.getStatus() == LoanStatus.REJECTED, "Return this book before deleting its loan.");
        require(!hasFine(loan), "This loan has a fine/payment record and cannot be deleted, even after payment.");
        if (loan.getStatus() == LoanStatus.REQUESTED) {
            BookCopy copy = locked(BookCopy.class, loan.getCopy().getId(), "Book copy");
            releasePendingCopy(copy, id);
        }
        publish(loan, "DELETE", "Loan record removed", "The desk removed the loan record for '"
                + loan.getCopy().getBook().getTitle() + "'.", NotificationType.INFO);
        loans.delete(loan);
        loans.flush(); // A linked-record failure rolls back this whole operation, including audit/notification.
    }

    public void refreshOverdue() {
        LocalDateTime now = LocalDateTime.now();
        for (Loan candidate : loans.findByStatusOrderByRequestedAtAsc(LoanStatus.ACTIVE)) {
            if (candidate.getDueAt() != null && candidate.getDueAt().isBefore(now)) {
                Loan loan = locked(Loan.class, candidate.getId(), "Loan");
                if (loan.getStatus() == LoanStatus.ACTIVE && loan.getDueAt() != null && loan.getDueAt().isBefore(now)) {
                    loan.setStatus(LoanStatus.OVERDUE);
                    loans.save(loan);
                }
            }
        }
    }

    private void activate(Loan loan) {
        LocalDateTime issued = LocalDateTime.now();
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setIssuedAt(issued);
        loan.setDueAt(dueAt(issued));
        loan.getCopy().setStatus(BookCopyStatus.LOANED);
        copies.save(loan.getCopy());
        loans.save(loan);
    }

    private void eligible(UserAccount member, Long bookId, Long excludedLoanId) {
        require(member.getRole() == Role.MEMBER && member.isActive(), "Select a registered active library member.");
        List<Loan> otherLoans = loans.findByMemberIdOrderByRequestedAtDesc(member.getId()).stream()
                .filter(l -> !l.getId().equals(excludedLoanId)).toList();
        require(otherLoans.stream().filter(l -> OPEN.contains(l.getStatus())).count() < 3,
                "This member already has 3 open requests/loans.");
        require(otherLoans.stream().noneMatch(l -> l.getStatus() == LoanStatus.OVERDUE
                || (l.getStatus() == LoanStatus.ACTIVE && l.getDueAt() != null && l.getDueAt().isBefore(LocalDateTime.now()))),
                "Return the member's overdue books before issuing another book.");
        require(!hasUnpaidFines(member.getId()), "Clear the member's unpaid fines before issuing another book.");
        require(otherLoans.stream().noneMatch(l -> OPEN.contains(l.getStatus()) && l.getCopy().getBook().getId().equals(bookId)),
                "This member already has an open request/loan for this title.");
    }

    private boolean hasUnpaidFines(Long memberId) {
        return fines.findByMemberIdOrderByIdDesc(memberId).stream()
                .anyMatch(f -> f.getStatus() == PaymentStatus.UNPAID && f.getAmount() != null && f.getAmount().signum() > 0);
    }

    private boolean hasFine(Loan loan) {
        return fines.findByMemberIdOrderByIdDesc(loan.getMember().getId()).stream()
                .anyMatch(f -> f.getLoan().getId().equals(loan.getId()));
    }

    private boolean noOtherOpenLoan(Long copyId, Long excludedLoanId) {
        return loans.findAll().stream().noneMatch(l -> l.getCopy().getId().equals(copyId)
                && !l.getId().equals(excludedLoanId) && OPEN.contains(l.getStatus()));
    }

    private void releasePendingCopy(BookCopy copy, Long excludedLoanId) {
        if (copy.getStatus() == BookCopyStatus.RESERVED && noOtherOpenLoan(copy.getId(), excludedLoanId)) {
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copies.save(copy);
        }
    }

    private <T> T locked(Class<T> type, Long id, String label) {
        require(id != null && id > 0, "Select a valid " + label.toLowerCase() + ".");
        T value = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        require(value != null, label + " was not found. Refresh the desk.");
        em.refresh(value, LockModeType.PESSIMISTIC_WRITE);
        return value;
    }

    private boolean isIssued(Loan loan) {
        return loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public static LocalDateTime dueAt(LocalDateTime issuedAt) { return issuedAt.plusDays(14); }

    public static BigDecimal overdueFine(LocalDateTime dueAt, LocalDateTime returnedAt) {
        if (dueAt == null) return BigDecimal.ZERO;
        long days = Math.max(0, ChronoUnit.DAYS.between(dueAt.toLocalDate(), returnedAt.toLocalDate()));
        return BigDecimal.valueOf(days * 25L);
    }

    private String issuedMessage(Loan loan) {
        return "'" + loan.getCopy().getBook().getTitle() + "' has been issued. Due " + loan.getDueAt().toLocalDate() + ".";
    }

    private void publish(Loan loan, String action, String title, String message, NotificationType type) {
        events.publish(new LoanEvent(loan, current.get().getEmail(), action, title, message, type));
    }
}
