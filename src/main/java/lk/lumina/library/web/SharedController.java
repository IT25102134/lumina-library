package lk.lumina.library.web;

import java.time.LocalDateTime;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SharedController {
    private final CurrentUserService current;private final UserAccountRepository users;private final NotificationRepository notifications;private final FinePaymentRepository fines;private final AuditLogRepository audits;private final BookRepository books;private final BookCopyRepository copies;private final LoanRepository loans;private final LibraryEventRepository events;private final EventRegistrationRepository registrations;private final FeedbackRepository feedback;private final AuditService audit;
    public SharedController(CurrentUserService current,UserAccountRepository users,NotificationRepository notifications,FinePaymentRepository fines,AuditLogRepository audits,BookRepository books,BookCopyRepository copies,LoanRepository loans,LibraryEventRepository events,EventRegistrationRepository registrations,FeedbackRepository feedback,AuditService audit){this.current=current;this.users=users;this.notifications=notifications;this.fines=fines;this.audits=audits;this.books=books;this.copies=copies;this.loans=loans;this.events=events;this.registrations=registrations;this.feedback=feedback;this.audit=audit;}
    @GetMapping("/profile") String profile(Model model){model.addAttribute("profile",current.get());return "profile";}
    @PostMapping("/profile") String saveProfile(@RequestParam String firstName,@RequestParam String lastName,@RequestParam(required=false) String phone,RedirectAttributes flash){UserAccount u=current.get();u.setFirstName(firstName.trim());u.setLastName(lastName.trim());u.setPhone(phone);users.save(u);audit.record(u.getEmail(),"UPDATE","Profile",u.getId(),"Personal details");flash.addFlashAttribute("success","Profile updated.");return "redirect:/profile";}
    @PostMapping("/notifications/{id}/read") String read(@PathVariable Long id,@RequestHeader(value="Referer",required=false) String referer){Notification n=notifications.findById(id).orElseThrow();if(n.getRecipient().getId().equals(current.get().getId())){n.setRead(true);notifications.save(n);}return "redirect:"+(referer==null?"/dashboard":referer);}
    @PreAuthorize("hasRole('MEMBER')") @PostMapping("/fines/{id}/pay") String pay(@PathVariable Long id,RedirectAttributes flash){FinePayment f=fines.findById(id).orElseThrow();if(!f.getMember().getId().equals(current.get().getId()))throw new IllegalArgumentException("Fine belongs to another member");f.setStatus(PaymentStatus.PAID);f.setPaidAt(LocalDateTime.now());f.setReference("WEB-"+System.currentTimeMillis());fines.save(f);notifications.save(new Notification(f.getMember(),"Payment confirmed","Payment "+f.getReference()+" was recorded successfully.",NotificationType.PAYMENT));audit.record(current.get().getEmail(),"PAY","Fine",id,f.getReference());flash.addFlashAttribute("success","Payment recorded successfully.");return "redirect:/my-library";}
    @PreAuthorize("hasAnyRole('HEAD_LIBRARIAN','LIBRARY_MANAGER','CIRCULATION_STAFF','LIBRARY_ASSISTANT','EVENT_COORDINATOR')") @GetMapping("/reports") String reports(Model model){model.addAttribute("bookCount",books.count());model.addAttribute("copyCount",copies.count());model.addAttribute("available",copies.countByStatus(BookCopyStatus.AVAILABLE));model.addAttribute("active",loans.countByStatus(LoanStatus.ACTIVE));model.addAttribute("returned",loans.countByStatus(LoanStatus.RETURNED));model.addAttribute("overdue",loans.countByStatusAndDueAtBefore(LoanStatus.ACTIVE,LocalDateTime.now()));model.addAttribute("events",events.count());model.addAttribute("registrations",registrations.count());model.addAttribute("feedback",feedback.count());model.addAttribute("members",users.findByRole(Role.MEMBER).size());return "reports";}
    @PreAuthorize("hasAnyRole('HEAD_LIBRARIAN','LIBRARY_MANAGER')") @GetMapping("/audit") String audit(Model model){model.addAttribute("logs",audits.findTop100ByOrderByOccurredAtDesc());return "audit";}
}
