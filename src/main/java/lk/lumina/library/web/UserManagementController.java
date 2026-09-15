package lk.lumina.library.web;

import lk.lumina.library.model.*;
import lk.lumina.library.repository.UserAccountRepository;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/users") @PreAuthorize("hasRole('LIBRARY_MANAGER')")
public class UserManagementController {
    private final UserAccountRepository users;private final PasswordEncoder encoder;private final AuditService audit;private final CurrentUserService current;
    public UserManagementController(UserAccountRepository users,PasswordEncoder encoder,AuditService audit,CurrentUserService current){this.users=users;this.encoder=encoder;this.audit=audit;this.current=current;}
    @GetMapping String page(Model model){model.addAttribute("users",users.findAll());model.addAttribute("roles",Role.values());return "users";}
    @PostMapping String create(@RequestParam String firstName,@RequestParam String lastName,@RequestParam String email,@RequestParam Role role,@RequestParam String membershipNumber,@RequestParam String password,RedirectAttributes flash){
        if(users.findByEmailIgnoreCase(email.trim()).isPresent()){flash.addFlashAttribute("error","An account already uses that email address.");return "redirect:/users";}
        UserAccount u=new UserAccount(firstName.trim(),lastName.trim(),email.trim().toLowerCase(),encoder.encode(password),role);u.setMembershipNumber(membershipNumber.trim());users.save(u);audit.record(current.get().getEmail(),"CREATE","User",u.getId(),u.getEmail()+" / "+role);flash.addFlashAttribute("success","User account created.");return "redirect:/users";
    }
    @PostMapping("/{id}/toggle") String toggle(@PathVariable Long id,RedirectAttributes flash){UserAccount u=users.findById(id).orElseThrow();if(u.getId().equals(current.get().getId())){flash.addFlashAttribute("error","You cannot deactivate your own signed-in account.");}else{u.setActive(!u.isActive());users.save(u);audit.record(current.get().getEmail(),u.isActive()?"ACTIVATE":"DEACTIVATE","User",id,u.getEmail());flash.addFlashAttribute("success","Account status updated.");}return "redirect:/users";}
}
