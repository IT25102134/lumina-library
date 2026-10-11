package lk.lumina.library.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired LoanRepository loans;
    @Autowired ReadingListRepository readingList;
    @Autowired FinePaymentRepository fines;
    @Autowired NotificationRepository notifications;

    private UserAccount testMember;

    @BeforeEach
    void setUp() {
        testMember = users.findByEmailIgnoreCase("member@lumina.lk").orElse(null);
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void dashboardRendersForMember() throws Exception {
        mvc.perform(get("/my-library"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Borrowing & requests")))
                .andExpect(content().string(containsString("Reading list")))
                .andExpect(content().string(containsString("Fines & payments")));
    }

    @Test
    void unauthenticatedAccessRedirectsToLogin() throws Exception {
        mvc.perform(get("/my-library"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void readingListLifecycle() throws Exception {
        mvc.perform(post("/my-library/reading-list/2").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalog"));

        List<ReadingListItem> items = readingList.findByMemberIdOrderByCreatedAtDesc(testMember.getId());
        org.junit.jupiter.api.Assertions.assertFalse(items.isEmpty());
        ReadingListItem item = items.get(0);

        mvc.perform(get("/my-library/reading-list/" + item.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Update reading goal")));

        mvc.perform(post("/my-library/reading-list/" + item.getId() + "/edit")
                        .param("status", "READING")
                        .param("priority", "HIGH")
                        .param("notes", "Chapter 4 in progress")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library"));

        ReadingListItem updated = readingList.findById(item.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ReadingStatus.READING, updated.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(ReadingPriority.HIGH, updated.getPriority());
        org.junit.jupiter.api.Assertions.assertEquals("Chapter 4 in progress", updated.getNotes());

        mvc.perform(post("/my-library/reading-list/" + item.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library"));
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void dedicatedReadingListPageAndExport() throws Exception {
        mvc.perform(get("/my-library/reading-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My Reading Journey")))
                .andExpect(content().string(containsString("Goal completion rate")));

        mvc.perform(get("/my-library/reading-list").param("status", "PLAN_TO_READ"))
                .andExpect(status().isOk());

        mvc.perform(get("/my-library/reading-list/save"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New reading list entry")));

        mvc.perform(get("/my-library/reading-list/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"reading-list.csv\"")));
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void borrowAndCancelFlow() throws Exception {
        mvc.perform(post("/my-library/borrow/3").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library"));

        List<Loan> memberLoans = loans.findByMemberIdOrderByRequestedAtDesc(testMember.getId());
        Loan requestedLoan = memberLoans.stream()
                .filter(l -> l.getStatus() == LoanStatus.REQUESTED && l.getCopy().getBook().getId().equals(3L))
                .findFirst()
                .orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(requestedLoan);

        mvc.perform(post("/my-library/loans/" + requestedLoan.getId() + "/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library"));

        Loan cancelledLoan = loans.findById(requestedLoan.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(LoanStatus.REJECTED, cancelledLoan.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(BookCopyStatus.AVAILABLE, cancelledLoan.getCopy().getStatus());
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void memberCardRenders() throws Exception {
        mvc.perform(get("/my-library/card"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MEM-2026-001")))
                .andExpect(content().string(containsString("Digital Member Card")));
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void borrowingHistoryAndExport() throws Exception {
        mvc.perform(get("/my-library/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Borrowing History")));

        mvc.perform(get("/my-library/history/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"borrowing-history.csv\"")));
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void memberNotifications() throws Exception {
        mvc.perform(get("/my-library/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Notifications")));

        mvc.perform(post("/my-library/notifications/read-all").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library/notifications"));
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void accountManagementAndSecurity() throws Exception {
        mvc.perform(get("/my-library/account"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Account Settings")));

        mvc.perform(post("/my-library/account/profile")
                        .param("firstName", "Maya")
                        .param("lastName", "Silva")
                        .param("phone", "0771234567")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library/account"));

        mvc.perform(post("/my-library/account/change-password")
                        .param("currentPassword", "WrongPass")
                        .param("newPassword", "NewPass123")
                        .param("confirmPassword", "NewPass123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library/account"))
                .andExpect(flash().attribute("error", "Current password is incorrect."));

        mvc.perform(get("/my-library/account/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Deactivate your account")));

        mvc.perform(post("/my-library/account/deactivate")
                        .param("password", "WrongPass")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library/account/deactivate"))
                .andExpect(flash().attribute("error", containsString("Password is incorrect")));
    }

    @Test
    @WithMockUser(username = "member@lumina.lk", roles = "MEMBER")
    void demoFinePayment() throws Exception {
        BookCopy copy = copies.findById(1L).orElseThrow();
        Loan loan = loans.save(new Loan(testMember, copy));
        FinePayment fine = new FinePayment();
        fine.setMember(testMember);
        fine.setLoan(loan);
        fine.setAmount(new BigDecimal("250.00"));
        fine.setStatus(PaymentStatus.UNPAID);
        fine = fines.save(fine);

        mvc.perform(post("/my-library/fines/" + fine.getId() + "/pay").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-library"));

        FinePayment paidFine = fines.findById(fine.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.PAID, paidFine.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(paidFine.getReference());
    }
}