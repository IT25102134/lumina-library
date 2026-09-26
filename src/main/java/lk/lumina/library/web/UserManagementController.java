package lk.lumina.library.web;

import java.math.BigDecimal;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/users") @PreAuthorize("hasRole('HEAD_LIBRARIAN')")
public class UserManagementController {
    private final UserAccountRepository users;private final MembershipFeeRepository fees;private final PasswordEncoder encoder;private final AuditService audit;private final CurrentUserService current;
    public UserManagementController(UserAccountRepository users,MembershipFeeRepository fees,PasswordEncoder encoder,AuditService audit,CurrentUserService current){this.users=users;this.fees=fees;this.encoder=encoder;this.audit=audit;this.current=current;}
    @GetMapping String page(Model model){model.addAttribute("users",users.findByRole(Role.MEMBER));return "users";}
    @Transactional @PostMapping String create(@RequestParam String firstName,@RequestParam String lastName,@RequestParam String email,@RequestParam String membershipNumber,@RequestParam String password,@RequestParam BigDecimal registrationFee,RedirectAttributes flash){
        if(users.findByEmailIgnoreCase(email.trim()).isPresent()){flash.addFlashAttribute("error","An account already uses that email address.");return "redirect:/users";}
        if(password.length()<8||registrationFee.signum()<0){flash.addFlashAttribute("error","Use a password of at least 8 characters and a valid registration fee.");return "redirect:/users";}
        UserAccount u=new UserAccount(firstName.trim(),lastName.trim(),email.trim().toLowerCase(),encoder.encode(password),Role.MEMBER);u.setMembershipNumber(membershipNumber.trim());users.save(u);
        fees.save(new MembershipFee(u,registrationFee,"REG-"+u.getId()));audit.record(current.get().getEmail(),"REGISTER_MEMBER","User",u.getId(),u.getEmail()+" / LKR "+registrationFee);flash.addFlashAttribute("success","Member registered. Their login username is "+u.getEmail()+".");return "redirect:/users";
    }
    @PostMapping("/{id}/toggle") String toggle(@PathVariable Long id,RedirectAttributes flash){UserAccount u=users.findById(id).orElseThrow();if(u.getId().equals(current.get().getId())){flash.addFlashAttribute("error","You cannot deactivate your own signed-in account.");}else{u.setActive(!u.isActive());users.save(u);audit.record(current.get().getEmail(),u.isActive()?"ACTIVATE":"DEACTIVATE","User",id,u.getEmail());flash.addFlashAttribute("success","Account status updated.");}return "redirect:/users";}
}
