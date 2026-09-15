package lk.lumina.library.web;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/circulation") @PreAuthorize("hasRole('CIRCULATION_STAFF')")
public class CirculationController {
    private final LoanRepository loans;private final BookCopyRepository copies;private final NotificationRepository notifications;private final FinePaymentRepository fines;private final AuditService audit;private final CurrentUserService current;
    public CirculationController(LoanRepository loans,BookCopyRepository copies,NotificationRepository notifications,FinePaymentRepository fines,AuditService audit,CurrentUserService current){this.loans=loans;this.copies=copies;this.notifications=notifications;this.fines=fines;this.audit=audit;this.current=current;}
    @GetMapping String page(Model model){refreshOverdue();var active=loans.findByStatusOrderByRequestedAtAsc(LoanStatus.ACTIVE);var overdue=loans.findByStatusOrderByRequestedAtAsc(LoanStatus.OVERDUE);var currentLoans=new java.util.ArrayList<Loan>(active);currentLoans.addAll(overdue);model.addAttribute("requests",loans.findByStatusOrderByRequestedAtAsc(LoanStatus.REQUESTED));model.addAttribute("activeLoans",active);model.addAttribute("overdueLoans",overdue);model.addAttribute("currentLoans",currentLoans);return "circulation";}
    @Transactional @PostMapping("/{id}/issue") String issue(@PathVariable Long id,RedirectAttributes flash){Loan l=loans.findById(id).orElseThrow();if(l.getStatus()!=LoanStatus.REQUESTED){flash.addFlashAttribute("error","This request has already been processed.");return "redirect:/circulation";}l.setStatus(LoanStatus.ACTIVE);l.setIssuedAt(LocalDateTime.now());l.setDueAt(LocalDateTime.now().plusDays(14));l.getCopy().setStatus(BookCopyStatus.LOANED);copies.save(l.getCopy());loans.save(l);notifications.save(new Notification(l.getMember(),"Book ready","“"+l.getCopy().getBook().getTitle()+"” has been issued. Due "+l.getDueAt().toLocalDate()+".",NotificationType.SUCCESS));log("ISSUE",l);flash.addFlashAttribute("success","Book issued successfully.");return "redirect:/circulation";}
    @Transactional @PostMapping("/{id}/reject") String reject(@PathVariable Long id,RedirectAttributes flash){Loan l=loans.findById(id).orElseThrow();l.setStatus(LoanStatus.REJECTED);l.getCopy().setStatus(BookCopyStatus.AVAILABLE);copies.save(l.getCopy());loans.save(l);notifications.save(new Notification(l.getMember(),"Request update","Your request for “"+l.getCopy().getBook().getTitle()+"” could not be approved.",NotificationType.WARNING));log("REJECT",l);flash.addFlashAttribute("success","Request rejected and copy released.");return "redirect:/circulation";}
    @Transactional @PostMapping("/{id}/return") String returned(@PathVariable Long id,RedirectAttributes flash){Loan l=loans.findById(id).orElseThrow();LocalDateTime returned=LocalDateTime.now();long overdue=l.getDueAt()==null?0:Math.max(0,ChronoUnit.DAYS.between(l.getDueAt().toLocalDate(),returned.toLocalDate()));l.setStatus(LoanStatus.RETURNED);l.setReturnedAt(returned);l.getCopy().setStatus(BookCopyStatus.AVAILABLE);copies.save(l.getCopy());loans.save(l);if(overdue>0){FinePayment fine=new FinePayment();fine.setLoan(l);fine.setMember(l.getMember());fine.setAmount(BigDecimal.valueOf(overdue*25L));fines.save(fine);notifications.save(new Notification(l.getMember(),"Overdue fine created","A fine of LKR "+fine.getAmount()+" was added for the returned book.",NotificationType.PAYMENT));}log("RETURN",l);flash.addFlashAttribute("success",overdue>0?"Book returned; overdue fine calculated.":"Book returned successfully.");return "redirect:/circulation";}
    private void refreshOverdue(){loans.findByStatusOrderByRequestedAtAsc(LoanStatus.ACTIVE).stream().filter(l->l.getDueAt()!=null&&l.getDueAt().isBefore(LocalDateTime.now())).forEach(l->{l.setStatus(LoanStatus.OVERDUE);loans.save(l);});}
    private void log(String action,Loan l){audit.record(current.get().getEmail(),action,"Loan",l.getId(),l.getCopy().getBook().getTitle()+" / "+l.getMember().getFullName());}
}
