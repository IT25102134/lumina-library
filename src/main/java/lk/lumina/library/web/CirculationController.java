package lk.lumina.library.web;

import java.math.BigDecimal;
import java.time.*;
import lk.lumina.library.model.*;
import lk.lumina.library.observer.LoanEvent;
import lk.lumina.library.observer.LoanEventPublisher;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import lk.lumina.library.strategy.LoanPolicy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/circulation") @PreAuthorize("hasRole('CIRCULATION_STAFF')")
public class CirculationController {
    private final LoanRepository loans;private final BookCopyRepository copies;private final FinePaymentRepository fines;private final CurrentUserService current;private final LoanPolicy loanPolicy;private final LoanEventPublisher loanEvents;
    public CirculationController(LoanRepository loans,BookCopyRepository copies,FinePaymentRepository fines,CurrentUserService current,LoanPolicy loanPolicy,LoanEventPublisher loanEvents){this.loans=loans;this.copies=copies;this.fines=fines;this.current=current;this.loanPolicy=loanPolicy;this.loanEvents=loanEvents;}
    @GetMapping String page(Model model){refreshOverdue();var active=loans.findByStatusOrderByRequestedAtAsc(LoanStatus.ACTIVE);var overdue=loans.findByStatusOrderByRequestedAtAsc(LoanStatus.OVERDUE);var currentLoans=new java.util.ArrayList<Loan>(active);currentLoans.addAll(overdue);model.addAttribute("requests",loans.findByStatusOrderByRequestedAtAsc(LoanStatus.REQUESTED));model.addAttribute("activeLoans",active);model.addAttribute("overdueLoans",overdue);model.addAttribute("currentLoans",currentLoans);return "circulation";}
    @Transactional @PostMapping("/{id}/issue") String issue(@PathVariable Long id,RedirectAttributes flash){Loan l=loans.findById(id).orElseThrow();if(l.getStatus()!=LoanStatus.REQUESTED){flash.addFlashAttribute("error","This request has already been processed.");return "redirect:/circulation";}l.setStatus(LoanStatus.ACTIVE);LocalDateTime issued=LocalDateTime.now();l.setIssuedAt(issued);l.setDueAt(loanPolicy.dueAt(issued));l.getCopy().setStatus(BookCopyStatus.LOANED);copies.save(l.getCopy());loans.save(l);publish(l,"ISSUE","Book ready","“"+l.getCopy().getBook().getTitle()+"” has been issued. Due "+l.getDueAt().toLocalDate()+".",NotificationType.SUCCESS);flash.addFlashAttribute("success","Book issued successfully.");return "redirect:/circulation";}
    @Transactional @PostMapping("/{id}/reject") String reject(@PathVariable Long id,RedirectAttributes flash){Loan l=loans.findById(id).orElseThrow();l.setStatus(LoanStatus.REJECTED);l.getCopy().setStatus(BookCopyStatus.AVAILABLE);copies.save(l.getCopy());loans.save(l);publish(l,"REJECT","Request update","Your request for “"+l.getCopy().getBook().getTitle()+"” could not be approved.",NotificationType.WARNING);flash.addFlashAttribute("success","Request rejected and copy released.");return "redirect:/circulation";}
    @Transactional @PostMapping("/{id}/return") String returned(@PathVariable Long id,RedirectAttributes flash){Loan l=loans.findById(id).orElseThrow();LocalDateTime returned=LocalDateTime.now();BigDecimal fineAmount=loanPolicy.overdueFine(l.getDueAt(),returned);l.setStatus(LoanStatus.RETURNED);l.setReturnedAt(returned);l.getCopy().setStatus(BookCopyStatus.AVAILABLE);copies.save(l.getCopy());loans.save(l);if(fineAmount.signum()>0){FinePayment fine=new FinePayment();fine.setLoan(l);fine.setMember(l.getMember());fine.setAmount(fineAmount);fines.save(fine);publish(l,"RETURN","Overdue fine created","A fine of LKR "+fine.getAmount()+" was added for the returned book.",NotificationType.PAYMENT);}else{publish(l,"RETURN",null,null,null);}flash.addFlashAttribute("success",fineAmount.signum()>0?"Book returned; overdue fine calculated.":"Book returned successfully.");return "redirect:/circulation";}
    private void refreshOverdue(){loans.findByStatusOrderByRequestedAtAsc(LoanStatus.ACTIVE).stream().filter(l->l.getDueAt()!=null&&l.getDueAt().isBefore(LocalDateTime.now())).forEach(l->{l.setStatus(LoanStatus.OVERDUE);loans.save(l);});}
    private void publish(Loan loan,String action,String title,String message,NotificationType type){loanEvents.publish(new LoanEvent(loan,current.get().getEmail(),action,title,message,type));}
}
