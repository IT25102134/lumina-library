package lk.lumina.library.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import lk.lumina.library.model.Role;
import lk.lumina.library.model.UserAccount;
import lk.lumina.library.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberRegistrationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void registrationPageRendersPublicly() throws Exception {
        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Join the library")))
                .andExpect(content().string(containsString("First name")))
                .andExpect(content().string(containsString("Email address")));
    }

    @Test
    void successfulRegistrationCreatesMember() throws Exception {
        String testEmail = "testreg" + System.currentTimeMillis() + "@lumina.lk";

        mvc.perform(post("/register")
                        .param("firstName", "Sunil")
                        .param("lastName", "Jayasinghe")
                        .param("email", testEmail)
                        .param("phone", "0712345678")
                        .param("password", "Pass@123456")
                        .param("confirmPassword", "Pass@123456")
                        .param("acceptTerms", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("success", containsString("Membership created")));

        UserAccount registered = users.findByEmailIgnoreCase(testEmail).orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(registered);
        org.junit.jupiter.api.Assertions.assertEquals(Role.MEMBER, registered.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(registered.isActive());
        org.junit.jupiter.api.Assertions.assertTrue(registered.getMembershipNumber().startsWith("MEM-"));
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Pass@123456", registered.getPassword()));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        mvc.perform(post("/register")
                        .param("firstName", "Duplicate")
                        .param("lastName", "User")
                        .param("email", "member@lumina.lk")
                        .param("phone", "0770000000")
                        .param("password", "Pass@123456")
                        .param("confirmPassword", "Pass@123456")
                        .param("acceptTerms", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }

    @Test
    void passwordMismatchIsRejected() throws Exception {
        mvc.perform(post("/register")
                        .param("firstName", "Mismatch")
                        .param("lastName", "User")
                        .param("email", "mismatch" + System.currentTimeMillis() + "@lumina.lk")
                        .param("phone", "0770000000")
                        .param("password", "Pass@123456")
                        .param("confirmPassword", "Different@123")
                        .param("acceptTerms", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));
    }
}