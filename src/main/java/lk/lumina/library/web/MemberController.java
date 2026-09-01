package lk.lumina.library.web;

import java.time.LocalDateTime;
import java.util.List;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/my-library") @PreAuthorize("hasRole('MEMBER')")
public class MemberController {
    private final CurrentUserService current;private final BookRepository books;private final BookCopyRepository copies;private final LoanRepository loans;
    private final ReadingListRepository reading;private final FinePaymentRepository fines;private final NotificationRepository notifications;private final AuditService audit;
    public MemberController(CurrentUserService current,BookRepository books,BookCopyRepository copies,LoanRepository loans,ReadingListRepository reading,FinePaymentRepository fines,NotificationRepository notifications,AuditService audit){this.current=current;this.books=books;this.copies=copies;this.loans=loans;this.reading=reading;this.fines=fines;this.notifications=notifications;this.audit=audit;}
    @GetMapping String page(Model model){UserAccount u=current.get();model.addAttribute("loans",loans.findByMemberIdOrderByRequestedAtDesc(u.getId()));model.addAttribute("readingList",reading.findByMemberIdOrderByCreatedAtDesc(u.getId()));model.addAttribute("fines",fines.findByMemberIdOrderByIdDesc(u.getId()));model.addAttribute("notifications",notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()));return "my-library";}
    @Transactional @PostMapping("/borrow/{bookId}") String borrow(@PathVariable Long bookId,RedirectAttributes flash){
        UserAccount u=current.get();
        if(loans.existsByMemberIdAndCopyBookIdAndStatusIn(u.getId(),bookId,List.of(LoanStatus.REQUESTED,LoanStatus.ACTIVE))){flash.addFlashAttribute("error","You already have an open request or loan for this title.");return "redirect:/catalog";}
        BookCopy copy=copies.findFirstByBookIdAndStatus(bookId,BookCopyStatus.AVAILABLE).orElse(null);
        if(copy==null){flash.addFlashAttribute("error","No copy is currently available.");return "redirect:/catalog";}
        copy.setStatus(BookCopyStatus.RESERVED);copies.save(copy);Loan loan=loans.save(new Loan(u,copy));
        notifications.save(new Notification(u,"Borrow request received","Your request for “"+copy.getBook().getTitle()+"” is awaiting desk approval.",NotificationType.INFO));
        audit.record(u.getEmail(),"BORROW_REQUEST","Loan",loan.getId(),copy.getBook().getTitle());flash.addFlashAttribute("success","Borrow request sent to the circulation desk.");return "redirect:/my-library";
    }
    @Transactional @PostMapping("/reading-list/{bookId}") String readingList(@PathVariable Long bookId,RedirectAttributes flash){UserAccount u=current.get();var existing=reading.findByMemberIdAndBookId(u.getId(),bookId);if(existing.isPresent()){reading.delete(existing.get());flash.addFlashAttribute("success","Removed from your reading list.");}else{reading.save(new ReadingListItem(u,books.findById(bookId).orElseThrow()));flash.addFlashAttribute("success","Saved to your reading list.");}return "redirect:/catalog";}
    @Transactional @PostMapping("/loans/{id}/renew") String renew(@PathVariable Long id,RedirectAttributes flash){Loan loan=loans.findById(id).orElseThrow();UserAccount u=current.get();if(!loan.getMember().getId().equals(u.getId())) throw new IllegalArgumentException("This loan belongs to another member.");if(loan.getStatus()!=LoanStatus.ACTIVE||loan.getRenewalCount()>=1||loan.getDueAt().isBefore(LocalDateTime.now())){flash.addFlashAttribute("error","This loan is not eligible for renewal.");}else{loan.setDueAt(loan.getDueAt().plusDays(7));loan.setRenewalCount(loan.getRenewalCount()+1);loans.save(loan);audit.record(u.getEmail(),"RENEW","Loan",id,loan.getCopy().getBook().getTitle());flash.addFlashAttribute("success","Loan renewed for 7 more days.");}return "redirect:/my-library";}
}
