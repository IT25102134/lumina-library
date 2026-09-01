package lk.lumina.library.web;

import java.time.LocalDateTime;
import java.util.*;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EventController {
    private final LibraryEventRepository events;private final EventRegistrationRepository registrations;private final CurrentUserService current;private final NotificationRepository notifications;private final AuditService audit;
    public EventController(LibraryEventRepository events,EventRegistrationRepository registrations,CurrentUserService current,NotificationRepository notifications,AuditService audit){this.events=events;this.registrations=registrations;this.current=current;this.notifications=notifications;this.audit=audit;}
    @GetMapping("/events") String page(Model model){List<LibraryEvent> list=events.findAllByOrderByStartAtDesc();Map<Long,Long> counts=new HashMap<>();list.forEach(e->counts.put(e.getId(),registrations.countByEventId(e.getId())));model.addAttribute("events",list);model.addAttribute("counts",counts);return "events";}
    @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @GetMapping("/events/manage/new") String form(Model model){model.addAttribute("event",new LibraryEvent());model.addAttribute("eventStatuses",EventStatus.values());return "event-form";}
    @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @GetMapping("/events/manage/{id}/edit") String edit(@PathVariable Long id,Model model){model.addAttribute("event",events.findById(id).orElseThrow());model.addAttribute("eventStatuses",EventStatus.values());return "event-form";}
    @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @PostMapping("/events/manage/save") String save(@ModelAttribute LibraryEvent input,RedirectAttributes flash){LibraryEvent e=input.getId()==null?new LibraryEvent():events.findById(input.getId()).orElseThrow();e.setTitle(input.getTitle());e.setDescription(input.getDescription());e.setLocation(input.getLocation());e.setStartAt(input.getStartAt());e.setCapacity(input.getCapacity());e.setStatus(input.getStatus()==null?EventStatus.PUBLISHED:input.getStatus());events.save(e);audit.record(current.get().getEmail(),input.getId()==null?"CREATE":"UPDATE","Event",e.getId(),e.getTitle());flash.addFlashAttribute("success","Event saved and published.");return "redirect:/events";}
    @Transactional @PreAuthorize("hasRole('MEMBER')") @PostMapping("/events/{id}/register") String register(@PathVariable Long id,RedirectAttributes flash){UserAccount u=current.get();LibraryEvent e=events.findById(id).orElseThrow();if(registrations.existsByEventIdAndMemberId(id,u.getId())){flash.addFlashAttribute("error","You are already registered for this event.");}else if(registrations.countByEventId(id)>=e.getCapacity()){flash.addFlashAttribute("error","This event is fully booked.");}else if(e.getStartAt().isBefore(LocalDateTime.now())||e.getStatus()!=EventStatus.PUBLISHED){flash.addFlashAttribute("error","Registration is closed.");}else{registrations.save(new EventRegistration(e,u));notifications.save(new Notification(u,"Event registration confirmed",e.getTitle()+" · "+e.getLocation(),NotificationType.EVENT));audit.record(u.getEmail(),"REGISTER","Event",id,e.getTitle());flash.addFlashAttribute("success","Your place is confirmed.");}return "redirect:/events";}
    @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @PostMapping("/events/manage/registration/{id}/attendance") String attendance(@PathVariable Long id){EventRegistration r=registrations.findById(id).orElseThrow();r.setAttended(!r.isAttended());registrations.save(r);return "redirect:/events";}
}
