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
public class FeedbackController {
    private final FeedbackRepository feedback;private final CurrentUserService current;private final NotificationRepository notifications;private final AuditService audit;
    public FeedbackController(FeedbackRepository feedback,CurrentUserService current,NotificationRepository notifications,AuditService audit){this.feedback=feedback;this.current=current;this.notifications=notifications;this.audit=audit;}
    @PreAuthorize("hasRole('MEMBER')") @GetMapping("/feedback") String member(Model model){model.addAttribute("items",feedback.findByMemberIdOrderByCreatedAtDesc(current.get().getId()));model.addAttribute("types",FeedbackType.values());return "feedback";}
    @PreAuthorize("hasRole('MEMBER')") @PostMapping("/feedback") String submit(@RequestParam FeedbackType type,@RequestParam String subject,@RequestParam String message,RedirectAttributes flash){FeedbackItem item=new FeedbackItem();item.setMember(current.get());item.setType(type);item.setSubject(subject.trim());item.setMessage(message.trim());feedback.save(item);audit.record(current.get().getEmail(),"SUBMIT","Feedback",item.getId(),subject);flash.addFlashAttribute("success","Your message was submitted with reference #"+item.getId()+".");return "redirect:/feedback";}
    @PreAuthorize("hasRole('LIBRARY_MANAGER')") @GetMapping("/feedback/manage") String manage(Model model){model.addAttribute("items",feedback.findAllByOrderByCreatedAtDesc());model.addAttribute("statuses",FeedbackStatus.values());return "feedback-manage";}
    @PreAuthorize("hasRole('LIBRARY_MANAGER')") @PostMapping("/feedback/manage/{id}") String respond(@PathVariable Long id,@RequestParam FeedbackStatus status,@RequestParam String response,RedirectAttributes flash){FeedbackItem item=feedback.findById(id).orElseThrow();item.setStatus(status);item.setResponse(response.trim());if(status==FeedbackStatus.RESOLVED||status==FeedbackStatus.CLOSED)item.setResolvedAt(LocalDateTime.now());feedback.save(item);notifications.save(new Notification(item.getMember(),"Feedback case updated","Your case #"+id+" is now "+status.name().replace('_',' ')+".",NotificationType.INFO));audit.record(current.get().getEmail(),"RESPOND","Feedback",id,status.name());flash.addFlashAttribute("success","Case updated and member notified.");return "redirect:/feedback/manage";}
}
