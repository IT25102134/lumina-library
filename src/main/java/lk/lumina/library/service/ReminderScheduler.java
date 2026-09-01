package lk.lumina.library.service;

import java.time.LocalDateTime;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReminderScheduler {
    private final LoanRepository loans;private final NotificationRepository notifications;
    public ReminderScheduler(LoanRepository loans,NotificationRepository notifications){this.loans=loans;this.notifications=notifications;}

    @Scheduled(cron="0 0 8 * * *",zone="Asia/Colombo")
    public void createDueDateReminders(){
        LocalDateTime now=LocalDateTime.now();
        loans.findByStatusOrderByRequestedAtAsc(LoanStatus.ACTIVE).stream()
            .filter(l->l.getDueAt()!=null&&l.getDueAt().isAfter(now)&&l.getDueAt().isBefore(now.plusDays(2)))
            .forEach(l->{String title="Book due soon";if(!notifications.existsByRecipientIdAndTitleAndCreatedAtAfter(l.getMember().getId(),title,now.minusHours(20)))notifications.save(new Notification(l.getMember(),title,"“"+l.getCopy().getBook().getTitle()+"” is due on "+l.getDueAt().toLocalDate()+".",NotificationType.WARNING));});
    }
}
