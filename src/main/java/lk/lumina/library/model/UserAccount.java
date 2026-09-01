package lk.lumina.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, length = 80) private String firstName;
    @NotBlank @Column(nullable = false, length = 80) private String lastName;
    @Email @NotBlank @Column(nullable = false, unique = true, length = 180) private String email;
    @Column(nullable = false) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private Role role;
    @Column(nullable = false) private boolean active = true;
    @Column(unique = true, length = 30) private String membershipNumber;
    @Column(length = 30) private String phone;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public UserAccount() {}
    public UserAccount(String firstName, String lastName, String email, String password, Role role) {
        this.firstName = firstName; this.lastName = lastName; this.email = email;
        this.password = password; this.role = role;
    }
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { lastName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { password = v; }
    public Role getRole() { return role; }
    public void setRole(Role v) { role = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String v) { membershipNumber = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getFullName() { return firstName + " " + lastName; }
}
