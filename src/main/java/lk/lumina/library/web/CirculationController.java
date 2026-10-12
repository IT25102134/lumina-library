package lk.lumina.library.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.CirculationService;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Only the Circulation desk uses this controller; shared module code is unchanged. */
@Controller
@RequestMapping("/circulation")
@PreAuthorize("hasRole('CIRCULATION_STAFF')")
public class CirculationController {
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
        flash.addFlashAttribute("success", "Book issued successfully.");
        return "redirect:/circulation";
    }

    @PostMapping("/{id}/reject")
    String reject(@PathVariable Long id, RedirectAttributes flash) {
        circulation.reject(id);
        flash.addFlashAttribute("success", "Request rejected and copy released.");
        return "redirect:/circulation";
    }

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
    }
}
