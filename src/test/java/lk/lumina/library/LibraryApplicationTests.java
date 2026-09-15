package lk.lumina.library;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import lk.lumina.library.model.LoanStatus;
import lk.lumina.library.repository.LoanRepository;
import lk.lumina.library.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest @AutoConfigureMockMvc
class LibraryApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired LoanRepository loans;
    @Autowired UserAccountRepository users;

    @Test void publicPagesAndSecurityWork() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Every story")));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("Step into Lumina")));
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test @WithMockUser(username="head@lumina.lk",roles="HEAD_LIBRARIAN")
    void headLibrarianOnlyHasCatalogueManagement() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isOk());
        mvc.perform(get("/catalog")).andExpect(status().isOk()).andExpect(content().string(containsString("A Tale of Two Cities")));
        mvc.perform(get("/catalog/manage/new")).andExpect(status().isOk());
        mvc.perform(get("/circulation")).andExpect(status().isForbidden());
        mvc.perform(get("/events/manage/new")).andExpect(status().isForbidden());
        mvc.perform(get("/feedback/manage")).andExpect(status().isForbidden());
        mvc.perform(get("/reports")).andExpect(status().isForbidden());
        mvc.perform(get("/audit")).andExpect(status().isForbidden());
        mvc.perform(get("/users")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username="member@lumina.lk",roles="MEMBER")
    void memberCanRequestAvailableBook() throws Exception {
        long before=loans.countByStatus(LoanStatus.REQUESTED);
        mvc.perform(post("/my-library/borrow/1").with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/my-library"));
        org.junit.jupiter.api.Assertions.assertEquals(before+1,loans.countByStatus(LoanStatus.REQUESTED));
        mvc.perform(get("/my-library")).andExpect(status().isOk()).andExpect(content().string(containsString("Borrowing & requests")));
        mvc.perform(get("/feedback")).andExpect(status().isOk());
    }

    @Test @WithMockUser(username="inventory@lumina.lk",roles="LIBRARY_ASSISTANT")
    void inventoryPageRenders() throws Exception { mvc.perform(get("/inventory")).andExpect(status().isOk()); }

    @Test @WithMockUser(username="circulation@lumina.lk",roles="CIRCULATION_STAFF")
    void circulationPageRenders() throws Exception { mvc.perform(get("/circulation")).andExpect(status().isOk()); }

    @Test @WithMockUser(username="events@lumina.lk",roles="EVENT_COORDINATOR")
    void eventPagesRender() throws Exception { mvc.perform(get("/events")).andExpect(status().isOk());mvc.perform(get("/events/manage/new")).andExpect(status().isOk()); }

    @Test @WithMockUser(username="manager@lumina.lk",roles="LIBRARY_MANAGER")
    void managerPagesRender() throws Exception { mvc.perform(get("/feedback/manage")).andExpect(status().isOk());mvc.perform(get("/reports")).andExpect(status().isOk());mvc.perform(get("/audit")).andExpect(status().isOk());mvc.perform(get("/users")).andExpect(status().isOk()); }
}
