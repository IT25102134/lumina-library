package lk.lumina.library.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
<<<<<<< Updated upstream
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.lumina.library.model.BookCopy;
import lk.lumina.library.model.BookCopyStatus;
import lk.lumina.library.model.FinePayment;
import lk.lumina.library.model.Loan;
import lk.lumina.library.model.LoanStatus;
import lk.lumina.library.model.NotificationType;
import lk.lumina.library.model.PaymentStatus;
import lk.lumina.library.model.Role;
import lk.lumina.library.model.UserAccount;
import lk.lumina.library.observer.LoanEvent;
import lk.lumina.library.observer.LoanEventPublisher;
import lk.lumina.library.repository.BookCopyRepository;
import lk.lumina.library.repository.FinePaymentRepository;
import lk.lumina.library.repository.LoanRepository;
import lk.lumina.library.repository.UserAccountRepository;
import lk.lumina.library.service.CurrentUserService;
import lk.lumina.library.strategy.LoanPolicy;
=======
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.CirculationService;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
>>>>>>> Stashed changes
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
<<<<<<< Updated upstream
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

=======
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Only the Circulation desk uses this controller; shared module code is unchanged. */
>>>>>>> Stashed changes
@Controller
@RequestMapping("/circulation")
@PreAuthorize("hasRole('CIRCULATION_STAFF')")
public class CirculationController {
<<<<<<< Updated upstream
    private static final int MAX_ACTIVE_LOANS = 3;
    private static final Set<LoanStatus> OPEN_LOAN_STATUSES =
        Set.of(LoanStatus.REQUESTED, LoanStatus.ACTIVE, LoanStatus.OVERDUE);

    private final LoanRepository loans;
    private final BookCopyRepository copies;
    private final FinePaymentRepository fines;
    private final UserAccountRepository users;
    private final CurrentUserService current;
    private final LoanPolicy loanPolicy;
    private final LoanEventPublisher loanEvents;

    public CirculationController(
        LoanRepository loans,
        BookCopyRepository copies,
        FinePaymentRepository fines,
        UserAccountRepository users,
        CurrentUserService current,
        LoanPolicy loanPolicy,
        LoanEventPublisher loanEvents
    ) {
        this.loans = loans;
        this.copies = copies;
        this.fines = fines;
        this.users = users;
        this.current = current;
        this.loanPolicy = loanPolicy;
        this.loanEvents = loanEvents;
    }

    @GetMapping
    String page(@RequestParam(name = "view", defaultValue = "issue") String view, Model model) {
        refreshOverdue();
        String selectedView = Set.of("issue", "renew", "overdue", "history").contains(view)
            ? view : "issue";

        List<Loan> requests = loans.findByStatusOrderByRequestedAtAsc(LoanStatus.REQUESTED);
        List<Loan> active = loans.findByStatusOrderByRequestedAtAsc(LoanStatus.ACTIVE);
        List<Loan> overdue = loans.findByStatusOrderByRequestedAtAsc(LoanStatus.OVERDUE);
        boolean overdueOnly = "overdue".equals(selectedView);
        List<Loan> currentLoans = new ArrayList<>(overdueOnly ? overdue : active);
        if (!overdueOnly) {
            currentLoans.addAll(overdue);
        }

        Map<Long, EligibilityResult> eligibilityByLoanId = new LinkedHashMap<>();
        for (Loan request : requests) {
            eligibilityByLoanId.put(request.getId(), requestEligibility(request));
        }

        List<UserAccount> eligibleMembers = users.findByRole(Role.MEMBER).stream()
            .filter(member -> memberEligibility(member, null, null).eligible())
            .toList();
        List<BookCopy> availableCopies = copies.findAllByOrderByBarcodeAsc().stream()
            .filter(copy -> copy.getStatus() == BookCopyStatus.AVAILABLE)
            .filter(copy -> copy.getBook().isActive())
            .toList();

        model.addAttribute("requests", requests);
        model.addAttribute("eligibilityByLoanId", eligibilityByLoanId);
        model.addAttribute("eligibleMembers", eligibleMembers);
        model.addAttribute("availableCopies", availableCopies);
        model.addAttribute("maxActiveLoans", MAX_ACTIVE_LOANS);
        model.addAttribute("activeLoans", active);
        model.addAttribute("overdueLoans", overdue);
        model.addAttribute("currentLoans", currentLoans);
        model.addAttribute("showOverdueOnly", overdueOnly);
        model.addAttribute("selectedView", selectedView);
        model.addAttribute("pageTitle", switch (selectedView) {
            case "renew" -> "Renew & Return";
            case "overdue" -> "Overdue Status";
            case "history" -> "Loan History";
            default -> "Issue & Requests";
        });
        model.addAttribute("sidebarActive", "circulation-" + selectedView);
        model.addAttribute("loanHistory", loans.findByStatusNotOrderByRequestedAtDesc(LoanStatus.REQUESTED));
        return "circulation";
    }

    @Transactional
    @PostMapping("/direct-issue")
    String directIssue(
        @RequestParam Long memberId,
        @RequestParam Long copyId,
        RedirectAttributes flash
    ) {
        UserAccount member = users.findById(memberId).orElse(null);
        BookCopy copy = copies.findById(copyId).orElse(null);
        if (member == null || copy == null) {
            flash.addFlashAttribute("error", "Choose a valid member and book copy.");
            return "redirect:/circulation";
        }

        EligibilityResult eligibility = memberEligibility(member, copy.getBook().getId(), null);
        if (!eligibility.eligible()) {
            flash.addFlashAttribute("error", "Loan cannot be created: " + eligibility.reason());
            return "redirect:/circulation";
        }
        if (!copy.getBook().isActive() || copy.getStatus() != BookCopyStatus.AVAILABLE) {
            flash.addFlashAttribute("error", "Loan cannot be created: the selected copy is not available.");
            return "redirect:/circulation";
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        Loan loan = new Loan(member, copy);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setIssuedAt(issuedAt);
        loan.setDueAt(loanPolicy.dueAt(issuedAt));
        copy.setStatus(BookCopyStatus.LOANED);
        copies.save(copy);
        loans.save(loan);
        publish(
            loan,
            "DIRECT_ISSUE",
            "Book issued",
            "“" + copy.getBook().getTitle() + "” has been issued at the circulation desk. Due "
                + loan.getDueAt().toLocalDate() + ".",
            NotificationType.SUCCESS
        );
        flash.addFlashAttribute("success", "New loan created and book issued successfully.");
        return "redirect:/circulation";
    }

    @Transactional
    @PostMapping("/{id}/issue")
    String issue(@PathVariable Long id, RedirectAttributes flash) {
        Loan loan = loans.findById(id).orElseThrow();
        if (loan.getStatus() != LoanStatus.REQUESTED) {
            flash.addFlashAttribute("error", "This request has already been processed.");
            return "redirect:/circulation";
        }

        EligibilityResult eligibility = requestEligibility(loan);
        if (!eligibility.eligible()) {
            flash.addFlashAttribute("error", "Request cannot be issued: " + eligibility.reason());
            return "redirect:/circulation";
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setIssuedAt(issuedAt);
        loan.setDueAt(loanPolicy.dueAt(issuedAt));
        loan.getCopy().setStatus(BookCopyStatus.LOANED);
        copies.save(loan.getCopy());
        loans.save(loan);
        publish(
            loan,
            "ISSUE",
            "Book ready",
            "“" + loan.getCopy().getBook().getTitle() + "” has been issued. Due "
                + loan.getDueAt().toLocalDate() + ".",
            NotificationType.SUCCESS
        );
=======
    private final LoanRepository loans;
    private final UserAccountRepository users;
    private final BookCopyRepository copies;
    private final FinePaymentRepository fines;
    private final CirculationService circulation;

    public CirculationController(LoanRepository loans, UserAccountRepository users,
            BookCopyRepository copies, FinePaymentRepository fines, CirculationService circulation) {
        this.loans = loans;
        this.users = users;
        this.copies = copies;
        this.fines = fines;
        this.circulation = circulation;
    }

    @GetMapping
    String page(Model model) {
        circulation.refreshOverdue();
        var all = loans.findAllByOrderByRequestedAtDesc();
        model.addAttribute("requests", all.stream().filter(l -> l.getStatus() == LoanStatus.REQUESTED).toList());
        model.addAttribute("currentLoans", all.stream().filter(l ->
                l.getStatus() == LoanStatus.ACTIVE || l.getStatus() == LoanStatus.OVERDUE).toList());
        model.addAttribute("history", all.stream().filter(l ->
                l.getStatus() == LoanStatus.RETURNED || l.getStatus() == LoanStatus.REJECTED).toList());
        model.addAttribute("members", users.findByRole(Role.MEMBER).stream()
                .filter(UserAccount::isActive).sorted(java.util.Comparator.comparing(UserAccount::getFullName)).toList());
        model.addAttribute("availableCopies", copies.findAllByOrderByBarcodeAsc().stream()
                .filter(c -> c.getStatus() == BookCopyStatus.AVAILABLE && c.getBook().isActive()).toList());
        model.addAttribute("fineLoanIds", fines.findAll().stream().map(f -> f.getLoan().getId()).toList());
        return "circulation";
    }

    // CREATE: existing member/copy IDs, without registering members or adding inventory.
    @PostMapping("/create")
    String create(@RequestParam Long memberId, @RequestParam Long copyId, RedirectAttributes flash) {
        var loan = circulation.create(memberId, copyId);
        flash.addFlashAttribute("success", "Loan #" + loan.getId() + " created and book issued. Due "
                + loan.getDueAt().toLocalDate() + ".");
        return "redirect:/circulation";
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable Long id, Model model) {
        model.addAttribute("loan", circulation.editable(id));
        return "circulation-edit";
    }

    // UPDATE: the original member/copy stay fixed; the due date is editable.
    @PostMapping("/{id}/update")
    String update(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt,
            RedirectAttributes flash) {
        circulation.updateDueDate(id, dueAt);
        flash.addFlashAttribute("success", "Loan due date updated.");
        return "redirect:/circulation";
    }

    @PostMapping("/{id}/renew")
    String renew(@PathVariable Long id, RedirectAttributes flash) {
        circulation.renew(id);
        flash.addFlashAttribute("success", "Loan renewed for 7 days.");
        return "redirect:/circulation";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id, RedirectAttributes flash) {
        circulation.delete(id);
        flash.addFlashAttribute("success", "Loan deleted. Any pending reserved copy has been released.");
        return "redirect:/circulation";
    }

    @PostMapping("/{id}/issue")
    String issue(@PathVariable Long id, RedirectAttributes flash) {
        circulation.issue(id);
>>>>>>> Stashed changes
        flash.addFlashAttribute("success", "Book issued successfully.");
        return "redirect:/circulation";
    }

<<<<<<< Updated upstream
    @Transactional
    @PostMapping("/{id}/reject")
    String reject(@PathVariable Long id, RedirectAttributes flash) {
        Loan loan = loans.findById(id).orElseThrow();
        if (loan.getStatus() != LoanStatus.REQUESTED) {
            flash.addFlashAttribute("error", "This request has already been processed.");
            return "redirect:/circulation";
        }
        loan.setStatus(LoanStatus.REJECTED);
        loan.getCopy().setStatus(BookCopyStatus.AVAILABLE);
        copies.save(loan.getCopy());
        loans.save(loan);
        publish(
            loan,
            "REJECT",
            "Request update",
            "Your request for “" + loan.getCopy().getBook().getTitle() + "” could not be approved.",
            NotificationType.WARNING
        );
=======
    @PostMapping("/{id}/reject")
    String reject(@PathVariable Long id, RedirectAttributes flash) {
        circulation.reject(id);
>>>>>>> Stashed changes
        flash.addFlashAttribute("success", "Request rejected and copy released.");
        return "redirect:/circulation";
    }

<<<<<<< Updated upstream
    @Transactional
    @PostMapping("/{id}/renew")
    String renew(@PathVariable Long id,
                 @RequestParam(defaultValue = "renew") String view, RedirectAttributes flash) {
        Loan loan = loans.findById(id).orElseThrow();
        if ((loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE)
            || loan.getRenewalCount() >= 1) {
            flash.addFlashAttribute("error", "This loan is not eligible for renewal.");
            return loanViewRedirect(view);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime renewalBase = loan.getDueAt() != null && loan.getDueAt().isAfter(now)
            ? loan.getDueAt()
            : now;
        loan.setDueAt(loanPolicy.dueAt(renewalBase));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loan.setStatus(LoanStatus.ACTIVE);
        loans.save(loan);
        publish(
            loan,
            "RENEW",
            "Loan renewed",
            "“" + loan.getCopy().getBook().getTitle() + "” was renewed. New due date: "
                + loan.getDueAt().toLocalDate() + ".",
            NotificationType.SUCCESS
        );
        flash.addFlashAttribute("success", "Loan renewed. The due date has been extended.");
        return loanViewRedirect(view);
    }

    @Transactional
    @PostMapping("/{id}/return")
    String returned(@PathVariable Long id,
                    @RequestParam(defaultValue = "renew") String view, RedirectAttributes flash) {
        Loan loan = loans.findById(id).orElseThrow();
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            flash.addFlashAttribute("error", "Only an active or overdue loan can be returned.");
            return loanViewRedirect(view);
        }
        LocalDateTime returnedAt = LocalDateTime.now();
        BigDecimal fineAmount = loanPolicy.overdueFine(loan.getDueAt(), returnedAt);
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(returnedAt);
        loan.getCopy().setStatus(BookCopyStatus.AVAILABLE);
        copies.save(loan.getCopy());
        loans.save(loan);

        if (fineAmount.signum() > 0) {
            FinePayment fine = new FinePayment();
            fine.setLoan(loan);
            fine.setMember(loan.getMember());
            fine.setAmount(fineAmount);
            fines.save(fine);
            publish(
                loan,
                "RETURN",
                "Overdue fine created",
                "A fine of LKR " + fine.getAmount() + " was added for the returned book.",
                NotificationType.PAYMENT
            );
        } else {
            publish(loan, "RETURN", null, null, null);
        }
        flash.addFlashAttribute(
            "success",
            fineAmount.signum() > 0
                ? "Book returned; overdue fine calculated."
                : "Book returned successfully."
        );
        return loanViewRedirect(view);
    }

    private String loanViewRedirect(String view) {
        return "redirect:/circulation?view=" + ("overdue".equals(view) ? "overdue" : "renew");
    }

    private EligibilityResult requestEligibility(Loan request) {
        EligibilityResult memberResult = memberEligibility(
            request.getMember(),
            request.getCopy().getBook().getId(),
            request.getId()
        );
        if (!memberResult.eligible()) {
            return memberResult;
        }
        if (!request.getCopy().getBook().isActive()) {
            return EligibilityResult.denied("Book is inactive");
        }
        if (request.getCopy().getStatus() != BookCopyStatus.RESERVED) {
            return EligibilityResult.denied("Reserved copy unavailable");
        }
        return EligibilityResult.allowed();
    }

    private EligibilityResult memberEligibility(UserAccount member, Long bookId, Long ignoredLoanId) {
        if (member == null || member.getRole() != Role.MEMBER) {
            return EligibilityResult.denied("Invalid member account");
        }
        if (!member.isActive()) {
            return EligibilityResult.denied("Member account is inactive");
        }
        if (member.getMembershipNumber() == null || member.getMembershipNumber().isBlank()) {
            return EligibilityResult.denied("Membership number is missing");
        }

        List<Loan> memberLoans = loans.findByMemberIdOrderByRequestedAtDesc(member.getId());
        boolean hasOverdueLoan = memberLoans.stream()
            .anyMatch(loan -> loan.getStatus() == LoanStatus.OVERDUE);
        if (hasOverdueLoan) {
            return EligibilityResult.denied("Member has an overdue book");
        }

        boolean hasUnpaidFine = fines.findByMemberIdOrderByIdDesc(member.getId()).stream()
            .anyMatch(fine -> fine.getStatus() == PaymentStatus.UNPAID);
        if (hasUnpaidFine) {
            return EligibilityResult.denied("Member has an unpaid fine");
        }

        long activeLoanCount = memberLoans.stream()
            .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE)
            .count();
        if (activeLoanCount >= MAX_ACTIVE_LOANS) {
            return EligibilityResult.denied("Maximum " + MAX_ACTIVE_LOANS + " active loans reached");
        }

        if (bookId != null) {
            boolean duplicateBook = memberLoans.stream()
                .filter(loan -> ignoredLoanId == null || !ignoredLoanId.equals(loan.getId()))
                .filter(loan -> OPEN_LOAN_STATUSES.contains(loan.getStatus()))
                .anyMatch(loan -> bookId.equals(loan.getCopy().getBook().getId()));
            if (duplicateBook) {
                return EligibilityResult.denied("Member already has this title open");
            }
        }

        return EligibilityResult.allowed();
    }

    private void refreshOverdue() {
        loans.findByStatusOrderByRequestedAtAsc(LoanStatus.ACTIVE).stream()
            .filter(loan -> loan.getDueAt() != null && loan.getDueAt().isBefore(LocalDateTime.now()))
            .forEach(loan -> {
                loan.setStatus(LoanStatus.OVERDUE);
                loans.save(loan);
            });
    }

    private void publish(
        Loan loan,
        String action,
        String title,
        String message,
        NotificationType type
    ) {
        loanEvents.publish(new LoanEvent(loan, current.get().getEmail(), action, title, message, type));
    }

    public record EligibilityResult(boolean eligible, String reason) {
        static EligibilityResult allowed() {
            return new EligibilityResult(true, "Eligible");
        }

        static EligibilityResult denied(String reason) {
            return new EligibilityResult(false, reason);
        }
=======
    @PostMapping("/{id}/return")
    String returned(@PathVariable Long id, RedirectAttributes flash) {
        BigDecimal fine = circulation.returned(id);
        flash.addFlashAttribute("success", fine.signum() > 0
                ? "Book returned; overdue fine of LKR " + fine + " calculated." : "Book returned successfully.");
        return "redirect:/circulation";
    }

    // Error handling applies to this controller only.
    @ExceptionHandler(IllegalArgumentException.class)
    String invalid(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/circulation";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    String invalidForm(Exception ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "Select a valid member/copy and enter a valid date and time.");
        return "redirect:/circulation";
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    String conflictingChange(Exception ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "This record changed or is linked to another record. Refresh the desk and try again.");
        return "redirect:/circulation";
    }

    // Keep the existing rule tests and original policy.
    static LocalDateTime dueAt(LocalDateTime issuedAt) { return CirculationService.dueAt(issuedAt); }
    static BigDecimal overdueFine(LocalDateTime dueAt, LocalDateTime returnedAt) {
        return CirculationService.overdueFine(dueAt, returnedAt);
>>>>>>> Stashed changes
    }
}
