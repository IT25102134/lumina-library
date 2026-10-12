package lk.lumina.library;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CirculationWorkflowTests {
    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired LoanRepository loans;
    @Autowired UserAccountRepository users;
    @Autowired FinePaymentRepository fines;

    @Test
    @WithMockUser(username = "circulation@lumina.lk", roles = "CIRCULATION_STAFF")
    void circulationStaffCanIssueRenewTrackOverdueReturnAndKeepHistory() throws Exception {
        Book book = books.save(new Book("9780000000042", "Circulation Test Book", "Lumina QA", "Testing"));
        BookCopy copy = new BookCopy(book, "CIRC-TEST-0042", "QA-01");
        copy.setStatus(BookCopyStatus.RESERVED);
        copy = copies.save(copy);
        UserAccount member = eligibleMember("renew-return");
        Loan loan = loans.save(new Loan(member, copy));

        mvc.perform(get("/circulation"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Approve &amp; Issue")));

        mvc.perform(post("/circulation/{id}/issue", loan.getId()).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/circulation"));
        loan = loans.findById(loan.getId()).orElseThrow();
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(BookCopyStatus.LOANED, loan.getCopy().getStatus());
        assertNotNull(loan.getIssuedAt());
        assertNotNull(loan.getDueAt());

        LocalDateTime firstDueDate = loan.getDueAt();
        mvc.perform(post("/circulation/{id}/renew", loan.getId()).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/circulation?view=renew"))
            .andExpect(flash().attribute("success", "Loan renewed. The due date has been extended."));
        loan = loans.findById(loan.getId()).orElseThrow();
        assertEquals(1, loan.getRenewalCount());
        assertTrue(loan.getDueAt().isAfter(firstDueDate));

        loan.setDueAt(LocalDateTime.now().minusDays(1));
        loans.saveAndFlush(loan);
        mvc.perform(get("/circulation").param("view", "overdue"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Overdue loans")))
            .andExpect(content().string(containsString("Circulation Test Book")))
            .andExpect(content().string(containsString("OVERDUE")));
        assertEquals(LoanStatus.OVERDUE, loans.findById(loan.getId()).orElseThrow().getStatus());

        int fineCountBeforeReturn = fines.findByMemberIdOrderByIdDesc(member.getId()).size();
        mvc.perform(post("/circulation/{id}/return", loan.getId()).param("view", "overdue").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/circulation?view=overdue"));
        loan = loans.findById(loan.getId()).orElseThrow();
        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertNotNull(loan.getReturnedAt());
        assertEquals(BookCopyStatus.AVAILABLE, loan.getCopy().getStatus());
        assertEquals(fineCountBeforeReturn + 1, fines.findByMemberIdOrderByIdDesc(member.getId()).size());

        mvc.perform(get("/circulation").param("view", "history"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Circulation Test Book")))
            .andExpect(content().string(containsString("RETURNED")));
    }

    @Test
    @WithMockUser(username = "circulation@lumina.lk", roles = "CIRCULATION_STAFF")
    void circulationStaffCanCreateDirectLoanForEligibleMember() throws Exception {
        Book book = books.save(new Book("9780000000043", "Direct Issue Test Book", "Lumina QA", "Testing"));
        BookCopy copy = copies.save(new BookCopy(book, "CIRC-TEST-0043", "QA-02"));
        UserAccount member = eligibleMember("direct-issue");

        mvc.perform(get("/circulation"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Direct issue / create loan")))
            .andExpect(content().string(containsString("Direct Issue Test Book")));

        mvc.perform(post("/circulation/direct-issue")
                .param("memberId", member.getId().toString())
                .param("copyId", copy.getId().toString())
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/circulation"))
            .andExpect(flash().attribute("success", "New loan created and book issued successfully."));

        Loan created = loans.findByMemberIdOrderByRequestedAtDesc(member.getId()).stream()
            .filter(loan -> loan.getCopy().getId().equals(copy.getId()))
            .findFirst()
            .orElseThrow();
        assertEquals(LoanStatus.ACTIVE, created.getStatus());
        assertNotNull(created.getIssuedAt());
        assertNotNull(created.getDueAt());
        assertEquals(BookCopyStatus.LOANED, copies.findById(copy.getId()).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "circulation@lumina.lk", roles = "CIRCULATION_STAFF")
    void overdueMemberIsShownAsIneligibleAndCannotBeIssuedAnotherBook() throws Exception {
        UserAccount member = eligibleMember("ineligible");

        Book overdueBook = books.save(new Book("9780000000044", "Overdue Eligibility Book", "Lumina QA", "Testing"));
        BookCopy overdueCopy = new BookCopy(overdueBook, "CIRC-TEST-0044", "QA-03");
        overdueCopy.setStatus(BookCopyStatus.LOANED);
        overdueCopy = copies.save(overdueCopy);
        Loan overdueLoan = new Loan(member, overdueCopy);
        overdueLoan.setStatus(LoanStatus.OVERDUE);
        overdueLoan.setIssuedAt(LocalDateTime.now().minusDays(20));
        overdueLoan.setDueAt(LocalDateTime.now().minusDays(6));
        loans.save(overdueLoan);

        Book requestedBook = books.save(new Book("9780000000045", "Blocked Request Book", "Lumina QA", "Testing"));
        BookCopy requestedCopy = new BookCopy(requestedBook, "CIRC-TEST-0045", "QA-04");
        requestedCopy.setStatus(BookCopyStatus.RESERVED);
        requestedCopy = copies.save(requestedCopy);
        Loan request = loans.save(new Loan(member, requestedCopy));

        mvc.perform(get("/circulation"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Member has an overdue book")));

        mvc.perform(post("/circulation/{id}/issue", request.getId()).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/circulation"))
            .andExpect(flash().attribute("error", "Request cannot be issued: Member has an overdue book"));

        request = loans.findById(request.getId()).orElseThrow();
        assertEquals(LoanStatus.REQUESTED, request.getStatus());
        assertEquals(BookCopyStatus.RESERVED, request.getCopy().getStatus());
    }

    private UserAccount eligibleMember(String suffix) {
        UserAccount member = new UserAccount(
            "Circulation",
            "Member " + suffix,
            "circulation-" + suffix + "@lumina.test",
            "test-password",
            Role.MEMBER
        );
        member.setMembershipNumber("CIRC-" + suffix.toUpperCase());
        return users.save(member);
    }

    @Test
    @WithMockUser(username = "circulation@lumina.lk", roles = "CIRCULATION_STAFF")
    void sidebarViewsRenderOnlyTheirOwnSections() throws Exception {
        mvc.perform(get("/circulation"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sidebarActive", "circulation-issue"))
            .andExpect(content().string(containsString("id=\"direct-issue\"")))
            .andExpect(content().string(containsString("id=\"borrow-requests\"")))
            .andExpect(content().string(not(containsString("id=\"current-loans\""))))
            .andExpect(content().string(not(containsString("id=\"loan-history\""))));

        for (String view : new String[]{"renew", "overdue"}) {
            mvc.perform(get("/circulation").param("view", view))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sidebarActive", "circulation-" + view))
                .andExpect(content().string(containsString("id=\"current-loans\"")))
                .andExpect(content().string(not(containsString("id=\"direct-issue\""))))
                .andExpect(content().string(not(containsString("id=\"borrow-requests\""))))
                .andExpect(content().string(not(containsString("id=\"loan-history\""))));
        }

        mvc.perform(get("/circulation").param("view", "history"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("sidebarActive", "circulation-history"))
            .andExpect(content().string(containsString("id=\"loan-history\"")))
            .andExpect(content().string(not(containsString("id=\"direct-issue\""))))
            .andExpect(content().string(not(containsString("id=\"borrow-requests\""))))
            .andExpect(content().string(not(containsString("id=\"current-loans\""))));

        mvc.perform(get("/circulation").param("view", "invalid"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("selectedView", "issue"));
    }

}
