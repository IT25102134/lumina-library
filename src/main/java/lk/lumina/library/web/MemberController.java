package lk.lumina.library.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.AuditService;
import lk.lumina.library.service.CurrentUserService;
import lk.lumina.library.web.form.ReadingListForm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for the Library Member / Member Services module.
 * Exclusively member-owned: provides Personal Dashboard, Reading List CRUD,
 * Borrowing requests, Loan renewal, Cancellation, History, Digital Card,
 * Notifications, Account management, and Demo fine payment.
 */
@Controller
@RequestMapping("/my-library")
@PreAuthorize("hasRole('MEMBER')")
public class MemberController {

    private static final int MAX_OPEN_LOANS = 3;

    private final CurrentUserService current;
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final ReadingListRepository reading;
    private final FinePaymentRepository fines;
    private final NotificationRepository notifications;
    private final AuditService audit;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public MemberController(CurrentUserService current,
                            BookRepository books,
                            BookCopyRepository copies,
                            LoanRepository loans,
                            ReadingListRepository reading,
                            FinePaymentRepository fines,
                            NotificationRepository notifications,
                            AuditService audit,
                            UserAccountRepository users,
                            PasswordEncoder passwordEncoder) {
        this.current = current;
        this.books = books;
        this.copies = copies;
        this.loans = loans;
        this.reading = reading;
        this.fines = fines;
        this.notifications = notifications;
        this.audit = audit;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================================
    // 1. Personal Member Dashboard (/my-library)
    // =========================================================================

    @GetMapping
    public String page(Model model) {
        UserAccount u = current.get();
        List<Loan> memberLoans = loans.findByMemberIdOrderByRequestedAtDesc(u.getId());
        List<ReadingListItem> memberReading = reading.findByMemberIdOrderByCreatedAtDesc(u.getId());
        List<FinePayment> memberFines = fines.findByMemberIdOrderByIdDesc(u.getId());
        List<Notification> memberNotifs = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId());

        BigDecimal unpaidTotal = memberFines.stream()
                .filter(f -> f.getStatus() == PaymentStatus.UNPAID)
                .map(FinePayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unreadCount = memberNotifs.stream().filter(n -> !n.isRead()).count();

        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("member", u);
        model.addAttribute("loans", memberLoans);
        model.addAttribute("readingList", memberReading);
        model.addAttribute("savedCount", memberReading.size());
        model.addAttribute("fines", memberFines);
        model.addAttribute("unpaidTotal", unpaidTotal);
        model.addAttribute("notifications", memberNotifs);
        model.addAttribute("unreadNotifsCount", unreadCount);
        model.addAttribute("now", LocalDateTime.now());
        return "my-library";
    }

    // =========================================================================
    // 2. Borrowing & Request Workflow
    // =========================================================================

    @Transactional
    @PostMapping("/borrow/{bookId}")
    public String borrow(@PathVariable Long bookId, RedirectAttributes flash) {
        UserAccount u = current.get();
        LocalDateTime now = LocalDateTime.now();

        // Check 1: Open loans limit (max 3 open loans/requests)
        List<Loan> memberLoans = loans.findByMemberIdOrderByRequestedAtDesc(u.getId());
        long openCount = memberLoans.stream()
                .filter(l -> l.getStatus() == LoanStatus.REQUESTED ||
                             l.getStatus() == LoanStatus.ACTIVE ||
                             l.getStatus() == LoanStatus.OVERDUE)
                .count();

        if (openCount >= MAX_OPEN_LOANS) {
            flash.addFlashAttribute("error",
                    "Borrowing limit reached: You already have " + openCount +
                    " open requests/loans (maximum allowed is " + MAX_OPEN_LOANS + ").");
            return "redirect:/catalog";
        }

        // Check 2: Overdue books block new requests
        boolean hasOverdue = memberLoans.stream().anyMatch(l ->
                l.getStatus() == LoanStatus.OVERDUE ||
                (l.getStatus() == LoanStatus.ACTIVE && l.getDueAt() != null && l.getDueAt().isBefore(now)));
        if (hasOverdue) {
            flash.addFlashAttribute("error",
                    "You cannot request new books while you have overdue loans. Please return your overdue books first.");
            return "redirect:/my-library";
        }

        // Check 3: Unpaid fines block new requests
        List<FinePayment> memberFines = fines.findByMemberIdOrderByIdDesc(u.getId());
        BigDecimal unpaid = memberFines.stream()
                .filter(f -> f.getStatus() == PaymentStatus.UNPAID)
                .map(FinePayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
            flash.addFlashAttribute("error",
                    "You cannot request new books while you have unpaid fines (LKR " + unpaid + "). Please clear your fines first.");
            return "redirect:/my-library";
        }

        // Check 4: Duplicate open request/loan for the same title
        if (loans.existsByMemberIdAndCopyBookIdAndStatusIn(u.getId(), bookId,
                List.of(LoanStatus.REQUESTED, LoanStatus.ACTIVE))) {
            flash.addFlashAttribute("error", "You already have an open request or active loan for this title.");
            return "redirect:/catalog";
        }

        // Check 5: Available copy in catalogue
        BookCopy copy = copies.findFirstByBookIdAndStatus(bookId, BookCopyStatus.AVAILABLE).orElse(null);
        if (copy == null) {
            flash.addFlashAttribute("error", "No copy is currently available for borrowing.");
            return "redirect:/catalog";
        }

        copy.setStatus(BookCopyStatus.RESERVED);
        copies.save(copy);

        Loan loan = loans.save(new Loan(u, copy));
        notifications.save(new Notification(u, "Borrow request received",
                "Your request for '" + copy.getBook().getTitle() + "' is awaiting desk approval.",
                NotificationType.INFO));
        audit.record(u.getEmail(), "BORROW_REQUEST", "Loan", loan.getId(), copy.getBook().getTitle());
        flash.addFlashAttribute("success", "Borrow request sent to the circulation desk.");
        return "redirect:/my-library";
    }

    /**
     * Cancel a pending REQUESTED loan.
     * Releases copy back to AVAILABLE.
     */
    @Transactional
    @PostMapping("/loans/{id}/cancel")
    public String cancelLoan(@PathVariable Long id, RedirectAttributes flash) {
        Loan loan = loans.findById(id).orElseThrow();
        UserAccount u = current.get();

        if (!loan.getMember().getId().equals(u.getId())) {
            throw new IllegalArgumentException("This loan belongs to another member.");
        }

        if (loan.getStatus() != LoanStatus.REQUESTED) {
            flash.addFlashAttribute("error", "Only pending requests can be cancelled.");
            return "redirect:/my-library";
        }

        BookCopy copy = loan.getCopy();
        if (copy != null) {
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copies.save(copy);
        }

        // Soft-cancel: status set to REJECTED with clear member audit
        loan.setStatus(LoanStatus.REJECTED);
        loans.save(loan);

        notifications.save(new Notification(u, "Borrow request cancelled",
                "Your borrow request for '" + (copy != null ? copy.getBook().getTitle() : "") + "' was cancelled.",
                NotificationType.INFO));
        audit.record(u.getEmail(), "BORROW_CANCEL", "Loan", id, "Cancelled by member");
        flash.addFlashAttribute("success", "Borrow request cancelled and copy released.");
        return "redirect:/my-library";
    }

    /**
     * Renew an ACTIVE loan for 7 days (max 1 renewal, not overdue, no unpaid fines).
     */
    @Transactional
    @PostMapping("/loans/{id}/renew")
    public String renew(@PathVariable Long id, RedirectAttributes flash) {
        Loan loan = loans.findById(id).orElseThrow();
        UserAccount u = current.get();
        LocalDateTime now = LocalDateTime.now();

        if (!loan.getMember().getId().equals(u.getId())) {
            throw new IllegalArgumentException("This loan belongs to another member.");
        }

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            flash.addFlashAttribute("error", "Only active loans can be renewed.");
            return "redirect:/my-library";
        }

        if (loan.getRenewalCount() >= 1) {
            flash.addFlashAttribute("error", "This loan has already reached the maximum renewal limit (1 renewal).");
            return "redirect:/my-library";
        }

        if (loan.getDueAt() != null && loan.getDueAt().isBefore(now)) {
            flash.addFlashAttribute("error", "Overdue loans cannot be renewed. Please return the book.");
            return "redirect:/my-library";
        }

        // Check for unpaid fines
        List<FinePayment> memberFines = fines.findByMemberIdOrderByIdDesc(u.getId());
        BigDecimal unpaid = memberFines.stream()
                .filter(f -> f.getStatus() == PaymentStatus.UNPAID)
                .map(FinePayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
            flash.addFlashAttribute("error", "Cannot renew while you have unpaid fines. Please clear fines first.");
            return "redirect:/my-library";
        }

        loan.setDueAt(loan.getDueAt().plusDays(7));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loans.save(loan);

        audit.record(u.getEmail(), "RENEW", "Loan", id, loan.getCopy().getBook().getTitle());
        flash.addFlashAttribute("success", "Loan renewed for 7 more days. New due date: " + loan.getDueAt().toLocalDate() + ".");
        return "redirect:/my-library";
    }

    // =========================================================================
    // 3. Reading List Full CRUD
    // =========================================================================

    /**
     * Dedicated Member Reading List Management Page (Full CRUD view).
     * Supports search query (q), status filtering, and priority filtering.
     */
    @GetMapping("/reading-list")
    public String readingListPage(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) ReadingStatus status,
                                  @RequestParam(required = false) ReadingPriority priority,
                                  Model model) {
        UserAccount u = current.get();
        List<ReadingListItem> allItems = reading.findByMemberIdOrderByCreatedAtDesc(u.getId());

        long totalCount = allItems.size();
        long planCount = allItems.stream().filter(i -> i.getStatus() == ReadingStatus.PLAN_TO_READ).count();
        long readingCount = allItems.stream().filter(i -> i.getStatus() == ReadingStatus.READING).count();
        long completedCount = allItems.stream().filter(i -> i.getStatus() == ReadingStatus.COMPLETED).count();
        int progressPct = totalCount > 0 ? (int) Math.round((double) completedCount * 100.0 / totalCount) : 0;

        List<ReadingListItem> filtered = allItems.stream()
                .filter(i -> {
                    if (q != null && !q.isBlank()) {
                        String query = q.trim().toLowerCase();
                        boolean matchTitle = i.getBook() != null && i.getBook().getTitle() != null && i.getBook().getTitle().toLowerCase().contains(query);
                        boolean matchAuthor = i.getBook() != null && i.getBook().getAuthor() != null && i.getBook().getAuthor().toLowerCase().contains(query);
                        boolean matchNotes = i.getNotes() != null && i.getNotes().toLowerCase().contains(query);
                        if (!matchTitle && !matchAuthor && !matchNotes) return false;
                    }
                    if (status != null && i.getStatus() != status) return false;
                    if (priority != null && i.getPriority() != priority) return false;
                    return true;
                })
                .toList();

        long unreadCount = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()).stream().filter(n -> !n.isRead()).count();

        model.addAttribute("activeTab", "reading-list");
        model.addAttribute("member", u);
        model.addAttribute("items", filtered);
        model.addAttribute("savedCount", totalCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("planCount", planCount);
        model.addAttribute("readingCount", readingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("progressPct", progressPct);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("unreadNotifsCount", unreadCount);
        model.addAttribute("q", q);
        model.addAttribute("statuses", ReadingStatus.values());
        model.addAttribute("priorities", ReadingPriority.values());
        return "reading-list";
    }

    @Transactional
    @PostMapping("/reading-list/{bookId}")
    public String toggleReadingList(@PathVariable Long bookId, RedirectAttributes flash) {
        UserAccount u = current.get();
        var existing = reading.findByMemberIdAndBookId(u.getId(), bookId);
        if (existing.isPresent()) {
            reading.delete(existing.get());
            flash.addFlashAttribute("success", "Removed from your reading list.");
        } else {
            Book book = books.findById(bookId).orElseThrow();
            ReadingListItem item = new ReadingListItem(u, book);
            reading.save(item);
            flash.addFlashAttribute("success", "Saved to your reading list.");
        }
        return "redirect:/catalog";
    }

    @GetMapping("/reading-list/save")
    public String newReadingListEntry(Model model) {
        UserAccount u = current.get();
        ReadingListForm form = new ReadingListForm();
        long unreadCount = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()).stream().filter(n -> !n.isRead()).count();
        long savedCount = reading.findByMemberIdOrderByCreatedAtDesc(u.getId()).size();
        model.addAttribute("activeTab", "reading-list");
        model.addAttribute("member", u);
        model.addAttribute("savedCount", savedCount);
        model.addAttribute("unreadNotifsCount", unreadCount);
        model.addAttribute("form", form);
        model.addAttribute("isNew", true);
        model.addAttribute("statuses", ReadingStatus.values());
        model.addAttribute("priorities", ReadingPriority.values());

        // Show books not already in reading list
        Set<Long> savedIds = reading.findByMemberIdOrderByCreatedAtDesc(u.getId())
                .stream().map(i -> i.getBook().getId()).collect(Collectors.toSet());
        List<Book> availableBooks = books.findByActiveTrueOrderByTitleAsc().stream()
                .filter(b -> !savedIds.contains(b.getId())).toList();
        model.addAttribute("availableBooks", availableBooks);
        return "reading-list-form";
    }

    @GetMapping("/reading-list/{id}/edit")
    public String editReadingListEntry(@PathVariable Long id, Model model) {
        UserAccount u = current.get();
        ReadingListItem item = reading.findByIdAndMemberId(id, u.getId())
                .orElseThrow(() -> new IllegalArgumentException("Reading list entry not found."));

        ReadingListForm form = new ReadingListForm();
        form.setBookId(item.getBook().getId());
        form.setStatus(item.getStatus());
        form.setPriority(item.getPriority());
        form.setNotes(item.getNotes());

        long unreadCount = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()).stream().filter(n -> !n.isRead()).count();
        long savedCount = reading.findByMemberIdOrderByCreatedAtDesc(u.getId()).size();
        model.addAttribute("activeTab", "reading-list");
        model.addAttribute("member", u);
        model.addAttribute("savedCount", savedCount);
        model.addAttribute("unreadNotifsCount", unreadCount);
        model.addAttribute("form", form);
        model.addAttribute("item", item);
        model.addAttribute("isNew", false);
        model.addAttribute("statuses", ReadingStatus.values());
        model.addAttribute("priorities", ReadingPriority.values());
        return "reading-list-form";
    }

    @Transactional
    @PostMapping("/reading-list/save")
    public String saveNewReadingEntry(@Valid @ModelAttribute("form") ReadingListForm form,
                                      BindingResult binding,
                                      @RequestParam(value = "returnTo", required = false) String returnTo,
                                      Model model,
                                      RedirectAttributes flash) {
        UserAccount u = current.get();
        if (binding.hasErrors()) {
            model.addAttribute("activeTab", "reading-list");
            model.addAttribute("member", u);
            model.addAttribute("isNew", true);
            model.addAttribute("statuses", ReadingStatus.values());
            model.addAttribute("priorities", ReadingPriority.values());
            Set<Long> savedIds = reading.findByMemberIdOrderByCreatedAtDesc(u.getId())
                    .stream().map(i -> i.getBook().getId()).collect(Collectors.toSet());
            model.addAttribute("availableBooks", books.findByActiveTrueOrderByTitleAsc().stream()
                    .filter(b -> !savedIds.contains(b.getId())).toList());
            return "reading-list-form";
        }

        Book book = books.findById(form.getBookId()).orElseThrow();
        ReadingListItem item = new ReadingListItem(u, book);
        if (form.getStatus() != null) item.setStatus(form.getStatus());
        if (form.getPriority() != null) item.setPriority(form.getPriority());
        item.setNotes(form.getNotes());
        reading.save(item);

        flash.addFlashAttribute("success", "Saved \"" + book.getTitle() + "\" to your reading list.");
        if ("reading-list".equals(returnTo)) {
            return "redirect:/my-library/reading-list";
        }
        return "redirect:/my-library";
    }

    @Transactional
    @PostMapping("/reading-list/{id}/edit")
    public String updateReadingEntry(@PathVariable Long id,
                                     @Valid @ModelAttribute("form") ReadingListForm form,
                                     BindingResult binding,
                                     @RequestParam(value = "returnTo", required = false) String returnTo,
                                     Model model,
                                     RedirectAttributes flash) {
        UserAccount u = current.get();
        ReadingListItem item = reading.findByIdAndMemberId(id, u.getId())
                .orElseThrow(() -> new IllegalArgumentException("Reading list entry not found."));

        if (binding.hasErrors()) {
            model.addAttribute("activeTab", "reading-list");
            model.addAttribute("member", u);
            model.addAttribute("isNew", false);
            model.addAttribute("item", item);
            model.addAttribute("statuses", ReadingStatus.values());
            model.addAttribute("priorities", ReadingPriority.values());
            return "reading-list-form";
        }

        if (form.getStatus() != null) item.setStatus(form.getStatus());
        if (form.getPriority() != null) item.setPriority(form.getPriority());
        item.setNotes(form.getNotes());
        reading.save(item);

        flash.addFlashAttribute("success", "Reading list entry updated.");
        if ("reading-list".equals(returnTo)) {
            return "redirect:/my-library/reading-list";
        }
        return "redirect:/my-library";
    }

    @Transactional
    @PostMapping("/reading-list/{id}/delete")
    public String deleteReadingEntry(@PathVariable Long id,
                                     @RequestParam(value = "returnTo", required = false) String returnTo,
                                     RedirectAttributes flash) {
        UserAccount u = current.get();
        ReadingListItem item = reading.findByIdAndMemberId(id, u.getId())
                .orElseThrow(() -> new IllegalArgumentException("Reading list entry not found."));
        String title = item.getBook().getTitle();
        reading.delete(item);
        flash.addFlashAttribute("success", "Removed \"" + title + "\" from your reading list.");
        if ("reading-list".equals(returnTo)) {
            return "redirect:/my-library/reading-list";
        }
        return "redirect:/my-library";
    }

    @GetMapping("/reading-list/export")
    public void exportReadingList(HttpServletResponse response) throws IOException {
        UserAccount u = current.get();
        List<ReadingListItem> list = reading.findByMemberIdOrderByCreatedAtDesc(u.getId());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"reading-list.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Title,Author,Category,Status,Priority,Notes,Date Added");
        for (ReadingListItem item : list) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    escapeCsv(item.getBook().getTitle()),
                    escapeCsv(item.getBook().getAuthor()),
                    escapeCsv(item.getBook().getCategory()),
                    item.getStatus() != null ? item.getStatus().getDisplayName() : "",
                    item.getPriority() != null ? item.getPriority().getDisplayName() : "",
                    escapeCsv(item.getNotes()),
                    item.getCreatedAt() != null ? item.getCreatedAt().toLocalDate() : "");
        }
        writer.flush();
    }

    // =========================================================================
    // 4. Digital Membership Card (/my-library/card)
    // =========================================================================

    @GetMapping("/card")
    public String membershipCard(Model model) {
        UserAccount u = current.get();
        long unreadCount = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()).stream().filter(n -> !n.isRead()).count();
        long savedCount = reading.findByMemberIdOrderByCreatedAtDesc(u.getId()).size();

        model.addAttribute("activeTab", "card");
        model.addAttribute("member", u);
        model.addAttribute("savedCount", savedCount);
        model.addAttribute("unreadNotifsCount", unreadCount);
        return "member-card";
    }

    // =========================================================================
    // 5. Personal Borrowing History (/my-library/history)
    // =========================================================================

    @GetMapping("/history")
    public String borrowingHistory(@RequestParam(required = false) String q, Model model) {
        UserAccount u = current.get();
        List<Loan> memberLoans = loans.findByMemberIdOrderByRequestedAtDesc(u.getId());

        if (q != null && !q.isBlank()) {
            String query = q.trim().toLowerCase();
            memberLoans = memberLoans.stream().filter(l ->
                    (l.getCopy() != null && l.getCopy().getBook() != null &&
                     (l.getCopy().getBook().getTitle().toLowerCase().contains(query) ||
                      l.getCopy().getBook().getAuthor().toLowerCase().contains(query))) ||
                    (l.getStatus() != null && l.getStatus().name().toLowerCase().contains(query))
            ).toList();
        }

        long unreadCount = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()).stream().filter(n -> !n.isRead()).count();
        long savedCount = reading.findByMemberIdOrderByCreatedAtDesc(u.getId()).size();

        model.addAttribute("activeTab", "history");
        model.addAttribute("member", u);
        model.addAttribute("history", memberLoans);
        model.addAttribute("savedCount", savedCount);
        model.addAttribute("unreadNotifsCount", unreadCount);
        model.addAttribute("q", q);
        return "member-history";
    }

    @GetMapping("/history/export")
    public void exportHistory(HttpServletResponse response) throws IOException {
        UserAccount u = current.get();
        List<Loan> memberLoans = loans.findByMemberIdOrderByRequestedAtDesc(u.getId());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"borrowing-history.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Title,Author,Barcode,Requested At,Issued At,Due At,Returned At,Status");
        for (Loan l : memberLoans) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    l.getCopy() != null && l.getCopy().getBook() != null ? escapeCsv(l.getCopy().getBook().getTitle()) : "",
                    l.getCopy() != null && l.getCopy().getBook() != null ? escapeCsv(l.getCopy().getBook().getAuthor()) : "",
                    l.getCopy() != null ? escapeCsv(l.getCopy().getBarcode()) : "",
                    l.getRequestedAt() != null ? l.getRequestedAt().toString() : "",
                    l.getIssuedAt() != null ? l.getIssuedAt().toString() : "",
                    l.getDueAt() != null ? l.getDueAt().toString() : "",
                    l.getReturnedAt() != null ? l.getReturnedAt().toString() : "",
                    l.getStatus() != null ? l.getStatus().name() : "");
        }
        writer.flush();
    }

    // =========================================================================
    // 6. Member Notifications (/my-library/notifications)
    // =========================================================================

    @GetMapping("/notifications")
    public String notificationsPage(Model model) {
        UserAccount u = current.get();
        List<Notification> notifs = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId());
        long unreadCount = notifs.stream().filter(n -> !n.isRead()).count();
        long savedCount = reading.findByMemberIdOrderByCreatedAtDesc(u.getId()).size();

        model.addAttribute("activeTab", "notifications");
        model.addAttribute("member", u);
        model.addAttribute("notifs", notifs);
        model.addAttribute("savedCount", savedCount);
        model.addAttribute("unreadNotifsCount", unreadCount);
        return "member-notifications";
    }

    @Transactional
    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id) {
        UserAccount u = current.get();
        Notification n = notifications.findById(id).orElseThrow();
        if (n.getRecipient().getId().equals(u.getId())) {
            n.setRead(true);
            notifications.save(n);
        }
        return "redirect:/my-library/notifications";
    }

    @Transactional
    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(RedirectAttributes flash) {
        UserAccount u = current.get();
        List<Notification> notifs = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId());
        for (Notification n : notifs) {
            if (!n.isRead()) {
                n.setRead(true);
                notifications.save(n);
            }
        }
        flash.addFlashAttribute("success", "All notifications marked as read.");
        return "redirect:/my-library/notifications";
    }

    // =========================================================================
    // 7. Fines & Demo Payments (/my-library/fines/{id}/pay)
    // =========================================================================

    @Transactional
    @PostMapping("/fines/{id}/pay")
    public String payFine(@PathVariable Long id, RedirectAttributes flash) {
        UserAccount u = current.get();
        FinePayment f = fines.findById(id).orElseThrow();
        if (!f.getMember().getId().equals(u.getId())) {
            throw new IllegalArgumentException("Fine belongs to another member.");
        }
        if (f.getStatus() == PaymentStatus.PAID) {
            flash.addFlashAttribute("info", "This fine has already been paid.");
            return "redirect:/my-library";
        }

        f.setStatus(PaymentStatus.PAID);
        f.setPaidAt(LocalDateTime.now());
        f.setReference("DEMO-" + System.currentTimeMillis());
        fines.save(f);

        notifications.save(new Notification(f.getMember(), "Payment confirmed",
                "Academic demo payment " + f.getReference() + " of LKR " + f.getAmount() + " recorded.",
                NotificationType.PAYMENT));
        audit.record(u.getEmail(), "PAY_FINE", "FinePayment", id, f.getReference());
        flash.addFlashAttribute("success", "Demo payment recorded successfully. Reference: " + f.getReference() + ".");
        return "redirect:/my-library";
    }

    // =========================================================================
    // 8. Account Settings & Deactivation (/my-library/account)
    // =========================================================================

    @GetMapping("/account")
    public String accountPage(Model model) {
        UserAccount u = current.get();
        long unreadCount = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()).stream().filter(n -> !n.isRead()).count();
        long savedCount = reading.findByMemberIdOrderByCreatedAtDesc(u.getId()).size();

        model.addAttribute("activeTab", "account");
        model.addAttribute("member", u);
        model.addAttribute("savedCount", savedCount);
        model.addAttribute("unreadNotifsCount", unreadCount);
        return "member-account";
    }

    @Transactional
    @PostMapping("/account/profile")
    public String updateProfile(@RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes flash) {
        UserAccount u = current.get();
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();

        if (fn.isBlank() || fn.length() > 80 || ln.isBlank() || ln.length() > 80) {
            flash.addFlashAttribute("error", "First name and last name are required (maximum 80 characters).");
            return "redirect:/my-library/account";
        }

        u.setFirstName(fn);
        u.setLastName(ln);
        u.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        users.save(u);

        audit.record(u.getEmail(), "UPDATE_PROFILE", "User", u.getId(), "Personal details updated");
        flash.addFlashAttribute("success", "Profile details updated.");
        return "redirect:/my-library/account";
    }

    @Transactional
    @PostMapping("/account/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpServletRequest request,
                                 RedirectAttributes flash) {
        UserAccount u = current.get();

        if (!passwordEncoder.matches(currentPassword, u.getPassword())) {
            flash.addFlashAttribute("error", "Current password is incorrect.");
            return "redirect:/my-library/account";
        }

        if (newPassword == null || newPassword.length() < 8) {
            flash.addFlashAttribute("error", "New password must be at least 8 characters.");
            return "redirect:/my-library/account";
        }

        if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("error", "New password and confirmation do not match.");
            return "redirect:/my-library/account";
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        users.save(u);
        audit.record(u.getEmail(), "CHANGE_PASSWORD", "User", u.getId(), "Password updated");

        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login?updated";
    }

    @Transactional
    @PostMapping("/account/change-email")
    public String changeEmail(@RequestParam String currentPassword,
                              @RequestParam String newEmail,
                              HttpServletRequest request,
                              RedirectAttributes flash) {
        UserAccount u = current.get();

        if (!passwordEncoder.matches(currentPassword, u.getPassword())) {
            flash.addFlashAttribute("error", "Current password is incorrect.");
            return "redirect:/my-library/account";
        }

        String email = newEmail == null ? "" : newEmail.trim().toLowerCase();
        if (email.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            flash.addFlashAttribute("error", "Enter a valid email address.");
            return "redirect:/my-library/account";
        }

        if (users.findByEmailIgnoreCase(email).filter(o -> !o.getId().equals(u.getId())).isPresent()) {
            flash.addFlashAttribute("error", "That email address is already in use.");
            return "redirect:/my-library/account";
        }

        u.setEmail(email);
        users.save(u);
        audit.record(u.getEmail(), "CHANGE_EMAIL", "User", u.getId(), "Email updated to " + email);

        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login?updated";
    }

    @GetMapping("/account/deactivate")
    public String deactivatePage(Model model) {
        UserAccount u = current.get();
        long unreadCount = notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()).stream().filter(n -> !n.isRead()).count();
        long savedCount = reading.findByMemberIdOrderByCreatedAtDesc(u.getId()).size();
        model.addAttribute("activeTab", "account");
        model.addAttribute("member", u);
        model.addAttribute("savedCount", savedCount);
        model.addAttribute("unreadNotifsCount", unreadCount);
        return "member-deactivate";
    }

    @Transactional
    @PostMapping("/account/deactivate")
    public String deactivateAccount(@RequestParam String password,
                                    HttpServletRequest request,
                                    RedirectAttributes flash) {
        UserAccount u = current.get();

        if (!passwordEncoder.matches(password, u.getPassword())) {
            flash.addFlashAttribute("error", "Password is incorrect. Deactivation cancelled.");
            return "redirect:/my-library/account/deactivate";
        }

        // Check for open loans or requests
        List<Loan> memberLoans = loans.findByMemberIdOrderByRequestedAtDesc(u.getId());
        boolean hasOpenLoans = memberLoans.stream().anyMatch(l ->
                l.getStatus() == LoanStatus.REQUESTED ||
                l.getStatus() == LoanStatus.ACTIVE ||
                l.getStatus() == LoanStatus.OVERDUE);
        if (hasOpenLoans) {
            flash.addFlashAttribute("error",
                    "Cannot deactivate: You have active, requested, or overdue loans. Please return books and cancel requests first.");
            return "redirect:/my-library/account/deactivate";
        }

        // Check for unpaid fines
        List<FinePayment> memberFines = fines.findByMemberIdOrderByIdDesc(u.getId());
        boolean hasUnpaidFines = memberFines.stream().anyMatch(f -> f.getStatus() == PaymentStatus.UNPAID);
        if (hasUnpaidFines) {
            flash.addFlashAttribute("error",
                    "Cannot deactivate: You have unpaid fines. Please clear all fines first.");
            return "redirect:/my-library/account/deactivate";
        }

        // Soft deactivation: set active = false
        u.setActive(false);
        users.save(u);
        audit.record(u.getEmail(), "DEACTIVATE", "User", u.getId(), "Account deactivated by member");

        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login?deactivated";
    }

    private static String escapeCsv(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
}