package lk.lumina.library.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final BookRepository books; private final BookCopyRepository copies; private final LoanRepository loans;
    private final LibraryEventRepository events; private final FeedbackRepository feedback; private final UserAccountRepository users;
    private final NotificationRepository notifications; private final CurrentUserService current;
    public HomeController(BookRepository books,BookCopyRepository copies,LoanRepository loans,LibraryEventRepository events,
                          FeedbackRepository feedback,UserAccountRepository users,NotificationRepository notifications,CurrentUserService current){
        this.books=books;this.copies=copies;this.loans=loans;this.events=events;this.feedback=feedback;this.users=users;this.notifications=notifications;this.current=current;
    }
    @GetMapping("/") String home(Model model){
        List<Book> featured=books.findByActiveTrueOrderByTitleAsc();
        model.addAttribute("featured",featured.stream().limit(4).toList());
        LocalDate today=LocalDate.now();
        List<LibraryEvent> published=events.findAllByOrderByStartAtDesc().stream()
                .filter(e->e.getStatus()==EventStatus.PUBLISHED && e.getStartAt()!=null).toList();
        List<LibraryEvent> todayEvents=published.stream()
                .filter(e->e.getStartAt().toLocalDate().isEqual(today))
                .sorted(Comparator.comparing(LibraryEvent::getStartAt)).toList();
        List<LibraryEvent> upcomingEvents=published.stream()
                .filter(e->e.getStartAt().toLocalDate().isAfter(today))
                .sorted(Comparator.comparing(LibraryEvent::getStartAt)).toList();
        model.addAttribute("todayEvents",todayEvents);
        model.addAttribute("upcomingEvents",upcomingEvents);
        model.addAttribute("nearestUpcomingEvents",upcomingEvents.stream().limit(3).toList());
        model.addAttribute("remainingUpcomingEvents",upcomingEvents.stream().skip(3).toList());
        model.addAttribute("upcoming",upcomingEvents);
        return "index";
    }
    @GetMapping("/login") String login(){return "login";}
    @GetMapping("/access-denied") String denied(){return "access-denied";}
    @GetMapping("/dashboard") String dashboard(Model model){
        UserAccount u=current.get();
        model.addAttribute("bookCount",books.count()); model.addAttribute("availableCount",copies.countByStatus(BookCopyStatus.AVAILABLE));
        model.addAttribute("activeLoanCount",loans.countByStatus(LoanStatus.ACTIVE)); model.addAttribute("requestCount",loans.countByStatus(LoanStatus.REQUESTED));
        model.addAttribute("memberCount",users.findByRole(Role.MEMBER).size());
        model.addAttribute("eventCount",events.findByStatusAndStartAtAfterOrderByStartAtAsc(EventStatus.PUBLISHED,LocalDateTime.now()).size());
        model.addAttribute("openFeedback",feedback.countByStatusIn(List.of(FeedbackStatus.OPEN,FeedbackStatus.IN_REVIEW)));
        model.addAttribute("overdueCount",loans.countByStatusAndDueAtBefore(LoanStatus.ACTIVE,LocalDateTime.now()));
        model.addAttribute("notifications",notifications.findTop8ByRecipientIdOrderByCreatedAtDesc(u.getId()));
        model.addAttribute("recentLoans",(u.getRole()==Role.MEMBER
                ? loans.findByMemberIdOrderByRequestedAtDesc(u.getId())
                : loans.findAllByOrderByRequestedAtDesc()).stream().limit(5).toList());
        model.addAttribute("upcoming",events.findByStatusAndStartAtAfterOrderByStartAtAsc(EventStatus.PUBLISHED,LocalDateTime.now()).stream().limit(3).toList());
        return "dashboard";
    }
}
