package lk.lumina.library.circulation.validation;

import jakarta.servlet.http.HttpServletRequest;
import lk.lumina.library.model.Role;
import lk.lumina.library.model.UserAccount;
import lk.lumina.library.repository.UserAccountRepository;
import lk.lumina.library.service.AuditService;
import lk.lumina.library.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Saves the Circulation Desk profile without changing the shared profile flow for other roles. */
@Controller
@RequestMapping("/circulation/profile")
@PreAuthorize("hasRole('CIRCULATION_STAFF')")
public class CirculationProfileController {
    private final CurrentUserService current;
    private final UserAccountRepository users;
    private final AuditService audit;
    private final PasswordEncoder encoder;

    CirculationProfileController(CurrentUserService current, UserAccountRepository users,
            AuditService audit, PasswordEncoder encoder) {
        this.current = current;
        this.users = users;
        this.audit = audit;
        this.encoder = encoder;
    }

    @PostMapping
    @Transactional
    String save(@RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam String staffNumberSuffix,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String confirmPassword,
            HttpServletRequest request,
            RedirectAttributes flash) {
        UserAccount user = current.get();
        if (user.getRole() != Role.CIRCULATION_STAFF) return "redirect:/access-denied";

        String error = CirculationProfileValidator.validate(
                firstName, lastName, email, phone, password, confirmPassword);
        if (error == null) {
            error = CirculationProfileValidator.validateStaffNumberSuffix(staffNumberSuffix);
        }
        if (error != null) {
            flash.addFlashAttribute("error", error);
            return "redirect:/profile";
        }

        String login = email.trim().toLowerCase();
        String staffNumber = "STAFF-" + staffNumberSuffix.trim();
        boolean duplicateEmail = users.findByEmailIgnoreCase(login)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .isPresent();
        if (duplicateEmail) {
            flash.addFlashAttribute("error", "That login username is already in use.");
            return "redirect:/profile";
        }
        boolean duplicateStaffNumber = users.findAll().stream()
                .anyMatch(existing -> !existing.getId().equals(user.getId())
                        && staffNumber.equalsIgnoreCase(existing.getMembershipNumber()));
        if (duplicateStaffNumber) {
            flash.addFlashAttribute("error", "That staff number is already in use.");
            return "redirect:/profile";
        }

        String newPassword = password == null ? "" : password;
        boolean credentialsChanged = !user.getEmail().equalsIgnoreCase(login) || !newPassword.isBlank();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(login);
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        user.setMembershipNumber(staffNumber);
        if (!newPassword.isBlank()) user.setPassword(encoder.encode(newPassword));
        users.save(user);
        audit.record(user.getEmail(), "UPDATE", "Profile", user.getId(),
                credentialsChanged ? "Credentials and circulation profile updated"
                        : "Circulation profile and staff number updated");

        if (credentialsChanged) {
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            SecurityContextHolder.clearContext();
            return "redirect:/login?updated";
        }
        flash.addFlashAttribute("success", "Profile updated.");
        return "redirect:/profile";
    }
}
