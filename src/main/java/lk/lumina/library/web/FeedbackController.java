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

    // Used to save and retrieve feedback cases .from the database.
    private final FeedbackRepository feedback;
    //Used to find the currently logged-in user.
    private final CurrentUserService current;
    // create notification to member
    private final NotificationRepository notifications;

    private final AuditService audit;
    // Constructor
    public FeedbackController(FeedbackRepository feedback,CurrentUserService current,NotificationRepository notifications,AuditService audit){
        this.feedback=feedback;
        this.current=current;
        this.notifications=notifications;
        this.audit=audit;}


    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/feedback")
    String member(Model model){
        //get only this member's feedback
        model.addAttribute("items",feedback.findByMemberIdOrderByCreatedAtDesc(current.get().getId()));
        //send feedback type
        model.addAttribute("types",FeedbackType.values());
        // display feedback.html
        return "feedback";}

    /*
    Memeber submit new feedback / complaint
     */
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/feedback")
    String submit(
            @RequestParam FeedbackType type,
            @RequestParam String subject,
            @RequestParam String message,
            RedirectAttributes flash) {FeedbackItem item=new FeedbackItem();item.setMember(current.get());item.setType(type);item.setSubject(subject.trim());item.setMessage(message.trim());feedback.save(item);audit.record(current.get().getEmail(),"SUBMIT","Feedback",item.getId(),subject);flash.addFlashAttribute("success","Your message was submitted with reference #"+item.getId()+".");return "redirect:/feedback";}
    /*
        Library manager can view every feedback case
     */
    @PreAuthorize("hasAnyRole('LIBRARY_MANAGER','HEAD_LIBRARIAN')")
    @GetMapping("/feedback/manage")
    String manage(Model model){
        //get all feedback cases
        model.addAttribute("items",feedback.findAllByOrderByCreatedAtDesc());
        //send available status values to HTML
        model.addAttribute("statuses",FeedbackStatus.values());
            return "feedback-manage";}

    /*
        manager response to feedback
     */
    @PreAuthorize("hasAnyRole('LIBRARY_MANAGER','HEAD_LIBRARIAN')")
    @PostMapping("/feedback/manage/{id}")
    String respond(@PathVariable Long id,
                   @RequestParam FeedbackStatus status,
                   @RequestParam String response,
                   RedirectAttributes flash) {
        FeedbackItem item=feedback.findById(id).orElseThrow();
        item.setStatus(status);     // update case status
        item.setResponse(response.trim()); // save manager response

        // if case is finished and save time and date
        if(status==FeedbackStatus.RESOLVED||status==FeedbackStatus.CLOSED)item.setResolvedAt(LocalDateTime.now());
        //save database
        feedback.save(item);
        //send notification to memeber
        notifications.save(new Notification(item.getMember(),"Feedback case updated","Your case #"+id+" is now "+status.name().replace('_',' ')+".",NotificationType.INFO));

        audit.record(current.get().getEmail(),"RESPOND","Feedback",id,status.name());

        //display success message
        flash.addFlashAttribute("success","Case updated and member notified.");

        return "redirect:/feedback/manage";} //return mange feeedback page
}
