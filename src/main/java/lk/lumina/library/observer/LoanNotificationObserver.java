package lk.lumina.library.observer;

import lk.lumina.library.model.Notification;
import lk.lumina.library.repository.NotificationRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Creates the same member notifications that circulation previously created directly. */
@Component @Order(1)
public class LoanNotificationObserver implements LoanEventObserver {
    private final NotificationRepository notifications;

    public LoanNotificationObserver(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override public void onLoanEvent(LoanEvent event) {
        if (event.notificationTitle() != null) {
            notifications.save(new Notification(event.loan().getMember(), event.notificationTitle(),
                event.notificationMessage(), event.notificationType()));
        }
    }
}
