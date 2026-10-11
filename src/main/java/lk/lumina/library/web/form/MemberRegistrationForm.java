package lk.lumina.library.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MemberRegistrationForm {
    @NotBlank(message = "First name is required.")
    @Size(max = 80, message = "First name must be 80 characters or fewer.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 80, message = "Last name must be 80 characters or fewer.")
    private String lastName;

    @NotBlank(message = "Email address is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 180, message = "Email must be 180 characters or fewer.")
    private String email;

    @Size(max = 30, message = "Phone number must be 30 characters or fewer.")
    private String phone;

    @NotBlank(message = "Password is required.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,72}$",
            message = "Use 10-72 characters with upper-case, lower-case and a number.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;

    private boolean acceptTerms;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public boolean isAcceptTerms() { return acceptTerms; }
    public void setAcceptTerms(boolean acceptTerms) { this.acceptTerms = acceptTerms; }
}