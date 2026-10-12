package lk.lumina.library.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EventController {
    private final LibraryEventRepository events;private final EventRegistrationRepository registrations;private final CurrentUserService current;private final NotificationRepository notifications;private final AuditService audit;
    public EventController(LibraryEventRepository events,EventRegistrationRepository registrations,CurrentUserService current,NotificationRepository notifications,AuditService audit){this.events=events;this.registrations=registrations;this.current=current;this.notifications=notifications;this.audit=audit;}
    @GetMapping("/events") String page(Model model){
        List<LibraryEvent> list=events.findAllByOrderByStartAtDesc();
        Map<Long,Long> counts=new HashMap<>();
        list.forEach(e->counts.put(e.getId(),registrations.countByEventId(e.getId())));
        LocalDate today=LocalDate.now();
        List<LibraryEvent> todayEvents=new ArrayList<>();
        List<LibraryEvent> upcomingEvents=new ArrayList<>();
        List<LibraryEvent> pastEvents=new ArrayList<>();
        for(LibraryEvent e:list){
            if(e.getStartAt()!=null){
                LocalDate d=e.getStartAt().toLocalDate();
                if(d.isEqual(today)){ todayEvents.add(e); }
                else if(d.isAfter(today)){ upcomingEvents.add(e); }
                else { pastEvents.add(e); }
            }
        }
        todayEvents.sort(Comparator.comparing(LibraryEvent::getStartAt));
        upcomingEvents.sort(Comparator.comparing(LibraryEvent::getStartAt));
        model.addAttribute("events",list);
        model.addAttribute("todayEvents",todayEvents);
        model.addAttribute("upcomingEvents",upcomingEvents);
        model.addAttribute("pastEvents",pastEvents);
        model.addAttribute("counts",counts);
        // Compute fully-booked set for all events
        Set<Long> fullyBookedIds=new HashSet<>();
        list.forEach(e->{ if(counts.getOrDefault(e.getId(),0L)>=e.getCapacity()) fullyBookedIds.add(e.getId()); });
        model.addAttribute("fullyBookedIds",fullyBookedIds);
        // Compute events the current member has already registered for
        Set<Long> registeredEventIds=new HashSet<>();
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MEMBER"))){
            try{ Long memberId=current.get().getId(); list.forEach(e->{ if(registrations.existsByEventIdAndMemberId(e.getId(),memberId)) registeredEventIds.add(e.getId()); }); }catch(Exception ignored){}
        }
        model.addAttribute("registeredEventIds",registeredEventIds);
        return "events";
    }
    @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @GetMapping("/events/manage/new") String form(Model model){model.addAttribute("event",new LibraryEvent());model.addAttribute("eventStatuses",EventStatus.values());model.addAttribute("minDateTime",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));return "event-form";}
    @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @GetMapping("/events/manage/{id}/edit") String edit(@PathVariable Long id,Model model,RedirectAttributes flash){LibraryEvent e=events.findById(id).orElseThrow();if(e.isPast()){flash.addFlashAttribute("error","Past events cannot be edited.");return "redirect:/events";}model.addAttribute("event",e);model.addAttribute("eventStatuses",EventStatus.values());model.addAttribute("minDateTime",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));return "event-form";}
    @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @PostMapping("/events/manage/save") String save(@ModelAttribute LibraryEvent input,RedirectAttributes flash){if(input.getId()!=null){LibraryEvent existing=events.findById(input.getId()).orElse(null);if(existing!=null && existing.isPast()){flash.addFlashAttribute("error","Past events cannot be edited.");return "redirect:/events";}}if(input.getStartAt()!=null && input.getStartAt().isBefore(LocalDateTime.now())){flash.addFlashAttribute("error","Event date and time must be from today onward.");return input.getId()==null?"redirect:/events/manage/new":"redirect:/events/manage/"+input.getId()+"/edit";}LibraryEvent e=input.getId()==null?new LibraryEvent():events.findById(input.getId()).orElseThrow();e.setTitle(input.getTitle());e.setDescription(input.getDescription());e.setLocation(input.getLocation());e.setStartAt(input.getStartAt());e.setCapacity(input.getCapacity());e.setStatus(input.getStatus()==null?EventStatus.PUBLISHED:input.getStatus());events.save(e);audit.record(current.get().getEmail(),input.getId()==null?"CREATE":"UPDATE","Event",e.getId(),e.getTitle());flash.addFlashAttribute("success","Event saved and published.");return "redirect:/events";}
    @Transactional @PreAuthorize("hasRole('MEMBER')") @PostMapping("/events/{id}/register") String register(@PathVariable Long id,RedirectAttributes flash){UserAccount u=current.get();LibraryEvent e=events.findById(id).orElseThrow();if(registrations.existsByEventIdAndMemberId(id,u.getId())){flash.addFlashAttribute("error","You have already reserved a place for this event.");}else if(registrations.countByEventId(id)>=e.getCapacity()){flash.addFlashAttribute("error","This event is fully booked — no seats remaining.");}else if(e.getStartAt().isBefore(LocalDateTime.now())||e.getStatus()!=EventStatus.PUBLISHED){flash.addFlashAttribute("error","Registration is closed.");}else{registrations.save(new EventRegistration(e,u));notifications.save(new Notification(u,"Event registration confirmed",e.getTitle()+" · "+e.getLocation(),NotificationType.EVENT));audit.record(u.getEmail(),"REGISTER","Event",id,e.getTitle());flash.addFlashAttribute("success","Your place is confirmed. See you there!");}return "redirect:/events";}
    @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @PostMapping("/events/manage/registration/{id}/attendance") String attendance(@PathVariable Long id){EventRegistration r=registrations.findById(id).orElseThrow();r.setAttended(!r.isAttended());registrations.save(r);return "redirect:/events";}
    @Transactional @PreAuthorize("hasAnyRole('EVENT_COORDINATOR','HEAD_LIBRARIAN')") @PostMapping("/events/manage/{id}/delete") String delete(@PathVariable Long id,RedirectAttributes flash){LibraryEvent e=events.findById(id).orElseThrow();String title=e.getTitle();registrations.deleteByEventId(id);events.deleteById(id);audit.record(current.get().getEmail(),"DELETE","Event",id,title);flash.addFlashAttribute("success","Event \""+title+"\" has been deleted.");return "redirect:/events";}
}
