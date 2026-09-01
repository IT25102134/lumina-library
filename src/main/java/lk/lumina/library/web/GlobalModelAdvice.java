package lk.lumina.library.web;

import lk.lumina.library.repository.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelAdvice {
    private final UserAccountRepository users; private final NotificationRepository notifications;
    public GlobalModelAdvice(UserAccountRepository users,NotificationRepository notifications){this.users=users;this.notifications=notifications;}
    @ModelAttribute
    public void shared(Authentication auth,Model model){
        if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)){
            users.findByEmailIgnoreCase(auth.getName()).ifPresent(u->{model.addAttribute("currentUser",u);model.addAttribute("unreadCount",notifications.countByRecipientIdAndReadFalse(u.getId()));});
        }
    }
}
