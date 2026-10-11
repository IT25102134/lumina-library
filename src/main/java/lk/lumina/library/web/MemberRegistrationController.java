package lk.lumina.library.web;

import jakarta.validation.Valid;
import java.time.Year;
import java.util.Objects;
import lk.lumina.library.model.Notification;
import lk.lumina.library.model.NotificationType;
import lk.lumina.library.model.Role;
import lk.lumina.library.model.UserAccount;
import lk.lumina.library.repository.NotificationRepository;
import lk.lumina.library.repository.UserAccountRepository;
import lk.lumina.library.service.AuditService;
import lk.lumina.library.web.form.MemberRegistrationForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MemberRegistrationController {
    private final UserAccountRepository users;
    private final NotificationRepository notifications;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public MemberRegistrationController(UserAccountRepository users,
            NotificationRepository notifications, PasswordEncoder passwordEncoder,
            AuditService audit) {
        this.users = users;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @GetMapping("/register")
    String registrationForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new MemberRegistrationForm());
        }
        return "register";
    }

    @Transactional
    @PostMapping("/register")
    String register(@Valid @ModelAttribute("form") MemberRegistrationForm form,
            BindingResult binding, RedirectAttributes flash) {
        String email = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            binding.rejectValue("email", "duplicate", "An account already uses this email address.");
        }
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        if (!form.isAcceptTerms()) {
            binding.rejectValue("acceptTerms", "required", "Accept the membership declaration to continue.");
        }
        if (binding.hasErrors()) {
            return "register";
        }

        UserAccount member = new UserAccount(
                form.getFirstName().trim(),
                form.getLastName().trim(),
                email,
                passwordEncoder.encode(form.getPassword()),
                Role.MEMBER);
        String phone = form.getPhone() == null ? null : form.getPhone().trim();
        member.setPhone(phone == null || phone.isBlank() ? null : phone);
        member = users.save(member);
        member.setMembershipNumber("MEM-" + Year.now().getValue() + "-" + String.format("%05d", member.getId()));
        users.save(member);

        notifications.save(new Notification(member, "Welcome to Lumina",
                "Your member account and digital library card are ready.", NotificationType.SUCCESS));
        audit.record(member.getEmail(), "SELF_REGISTER", "User", member.getId(), member.getMembershipNumber());
        flash.addFlashAttribute("success", "Membership created. Sign in with your new account.");
        return "redirect:/login";
    }
}
