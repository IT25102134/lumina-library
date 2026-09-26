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

    @Autowired lk.lumina.library.repository.LibraryEventRepository events;

    @Test @WithMockUser(username="events@lumina.lk",roles="EVENT_COORDINATOR")
    void pastEventsCannotBeEditedAndEditOptionRemoved() throws Exception {
        lk.lumina.library.model.LibraryEvent past = new lk.lumina.library.model.LibraryEvent();
        past.setTitle("Past Poetry Workshop");
        past.setDescription("A workshop held last month.");
        past.setLocation("Hall B");
        past.setStartAt(java.time.LocalDateTime.now().minusDays(5));
        past.setCapacity(20);
        past = events.save(past);

        // Events list should show the past event and delete button, but NOT an edit button for it
        mvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Past Poetry Workshop")))
                .andExpect(content().string(containsString("/events/manage/" + past.getId() + "/delete")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/events/manage/" + past.getId() + "/edit"))));

        // Direct GET to edit page for past event should redirect with flash message
        mvc.perform(get("/events/manage/" + past.getId() + "/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"))
                .andExpect(flash().attribute("error", "Past events cannot be edited."));

        // Attempting to POST save for past event should redirect with flash message
        mvc.perform(post("/events/manage/save")
                        .param("id", past.getId().toString())
                        .param("title", "Updated Title")
                        .param("description", "Updated Desc")
                        .param("location", "Hall B")
                        .param("startAt", java.time.LocalDateTime.now().plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")))
                        .param("capacity", "25")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"))
                .andExpect(flash().attribute("error", "Past events cannot be edited."));
    }

    @Test @WithMockUser(username="manager@lumina.lk",roles="LIBRARY_MANAGER")
    void managerPagesRender() throws Exception { mvc.perform(get("/feedback/manage")).andExpect(status().isOk());mvc.perform(get("/reports")).andExpect(status().isOk());mvc.perform(get("/audit")).andExpect(status().isOk());mvc.perform(get("/users")).andExpect(status().isOk()); }
}
