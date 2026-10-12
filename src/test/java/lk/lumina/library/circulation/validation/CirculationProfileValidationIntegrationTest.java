package lk.lumina.library.circulation.validation;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import lk.lumina.library.model.Role;
import lk.lumina.library.model.UserAccount;
import lk.lumina.library.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:circulation-profile;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "app.open-browser=false"
})
@ActiveProfiles("circulation-profile-test")
@AutoConfigureMockMvc
@Transactional
class CirculationProfileValidationIntegrationTest {
    private static final String CIRCULATION_EMAIL = "circulation-profile@lumina.lk";
    private static final String MANAGER_EMAIL = "manager-profile@lumina.lk";

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;

    @BeforeEach
    void setUp() {
        users.findByEmailIgnoreCase(CIRCULATION_EMAIL).orElseGet(() -> {
            UserAccount account = new UserAccount(
                    "Nimal", "Fernando", CIRCULATION_EMAIL, "encoded", Role.CIRCULATION_STAFF);
            account.setMembershipNumber("PROFILE-CIRC-001");
            return users.saveAndFlush(account);
        });
        users.findByEmailIgnoreCase(MANAGER_EMAIL).orElseGet(() -> {
            UserAccount account = new UserAccount(
                    "Library", "Manager", MANAGER_EMAIL, "encoded", Role.LIBRARY_MANAGER);
            account.setMembershipNumber("PROFILE-MGR-001");
            return users.saveAndFlush(account);
        });
    }

    @Test
    @WithMockUser(username = CIRCULATION_EMAIL, roles = "CIRCULATION_STAFF")
    void invalidCirculationProfileIsRejectedWithoutSaving() throws Exception {
        mvc.perform(post("/circulation/profile").with(csrf())
                        .param("firstName", "Nimal435436")
                        .param("lastName", "Fernando233121435")
                        .param("email", CIRCULATION_EMAIL)
                        .param("phone", "+94 11 234 567efg")
                        .param("staffNumberSuffix", "002")
                        .param("password", "")
                        .param("confirmPassword", ""))
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("error", containsString("First name")));

        UserAccount unchanged = users.findByEmailIgnoreCase(CIRCULATION_EMAIL).orElseThrow();
        assertEquals("Nimal", unchanged.getFirstName());
        assertEquals("Fernando", unchanged.getLastName());
    }

    @Test
    @WithMockUser(username = CIRCULATION_EMAIL, roles = "CIRCULATION_STAFF")
    void validCirculationProfileStillUsesExistingSaveFlow() throws Exception {
        mvc.perform(post("/circulation/profile").with(csrf())
                        .param("firstName", "Nimal-Anura")
                        .param("lastName", "Fernando")
                        .param("email", CIRCULATION_EMAIL)
                        .param("phone", "+94 77 123 4567")
                        .param("staffNumberSuffix", "025")
                        .param("password", "")
                        .param("confirmPassword", ""))
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("success", "Profile updated."));

        UserAccount updated = users.findByEmailIgnoreCase(CIRCULATION_EMAIL).orElseThrow();
        assertEquals("Nimal-Anura", updated.getFirstName());
        assertEquals("+94 77 123 4567", updated.getPhone());
        assertEquals("STAFF-025", updated.getMembershipNumber());
    }

    @Test
    void circulationStaffGetsTheLiveValidatedProfileView() throws Exception {
        mvc.perform(get("/profile").with(user(CIRCULATION_EMAIL).roles("CIRCULATION_STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("circulation/profile"))
                .andExpect(content().string(containsString("data-circulation-profile")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("data-password-strength"))))
                .andExpect(content().string(containsString("/js/circulation/profile-validation.js")));
    }

    @Test
    void circulationDashboardMovesReviewRequestsIntoTheSidebar() throws Exception {
        mvc.perform(get("/dashboard").with(user(CIRCULATION_EMAIL).roles("CIRCULATION_STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("circulation/dashboard"))
                .andExpect(content().string(containsString("<nav class=\"nav-stack\">")))
                .andExpect(content().string(containsString("Review requests")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("quick-actions"))));
    }

    @Test
    void anotherRoleKeepsTheOriginalSharedDashboardView() throws Exception {
        mvc.perform(get("/dashboard").with(user(MANAGER_EMAIL).roles("LIBRARY_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void anotherRoleKeepsTheOriginalSharedProfileView() throws Exception {
        mvc.perform(get("/profile").with(user(MANAGER_EMAIL).roles("LIBRARY_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("data-circulation-profile"))));
    }

    @Test
    void anotherRoleKeepsTheOriginalSharedProfileBehavior() throws Exception {
        mvc.perform(post("/profile").with(user(MANAGER_EMAIL).roles("LIBRARY_MANAGER")).with(csrf())
                        .param("firstName", "Manager123")
                        .param("lastName", "UnchangedFlow")
                        .param("email", MANAGER_EMAIL)
                        .param("phone", "existing-format")
                        .param("password", "")
                        .param("confirmPassword", ""))
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("success", "Profile updated."));

        assertEquals("Manager123", users.findByEmailIgnoreCase(MANAGER_EMAIL).orElseThrow().getFirstName());
    }
}
