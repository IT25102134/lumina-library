package lk.lumina.library.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Runs against the existing isolated H2 test configuration, never the user's SQL Server. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:circulation-crud;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "app.open-browser=false"
})
@ActiveProfiles("circulation-test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "circulation@lumina.lk", roles = "CIRCULATION_STAFF")
class CirculationCrudIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired LoanRepository loans;
    @Autowired FinePaymentRepository fines;
    @Autowired AuditLogRepository audit;
    private UserAccount member;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        users.findByEmailIgnoreCase("circulation@lumina.lk").orElseGet(() -> users.save(
                new UserAccount("Test", "Staff", "circulation@lumina.lk", "encoded", Role.CIRCULATION_STAFF)));
        member = new UserAccount("CRUD", "Member", "circulation-crud@lumina.lk", "encoded", Role.MEMBER);
        member.setMembershipNumber("CIRC-TEST-001");
        member = users.saveAndFlush(member);
        copy = newCopy("CIRC-ISBN-1", "CIRC-COPY-1");
    }

    @Test
    void createReadEditReturnAndDeleteUseExistingRows() throws Exception {
        Loan loan = create();
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(loan.getIssuedAt().plusDays(14), loan.getDueAt());
        assertEquals(BookCopyStatus.LOANED, copies.findById(copy.getId()).orElseThrow().getStatus());
        assertEquals(member.getId(), loan.getMember().getId());
        assertEquals(copy.getId(), loan.getCopy().getId());
        mvc.perform(get("/circulation"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New loan / Issue book")))
                .andExpect(content().string(containsString("CIRC-COPY-1")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        String minimumDue = loan.getIssuedAt().plusMinutes(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        mvc.perform(get("/circulation/" + loan.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Save changes")))
                .andExpect(content().string(containsString("min=\"" + minimumDue + "\"")))
                .andExpect(content().string(containsString("/js/circulation/due-date-validation.js")));
        LocalDateTime newDue = LocalDateTime.now().plusDays(20).withNano(0);
        mvc.perform(post("/circulation/" + loan.getId() + "/update").with(csrf())
                .param("dueAt", newDue.toString())).andExpect(flash().attributeExists("success"));
        assertEquals(newDue, loans.findById(loan.getId()).orElseThrow().getDueAt());
        mvc.perform(post("/circulation/" + loan.getId() + "/return").with(csrf()))
                .andExpect(flash().attributeExists("success"));
        assertEquals(LoanStatus.RETURNED, loans.findById(loan.getId()).orElseThrow().getStatus());
        assertEquals(BookCopyStatus.AVAILABLE, copies.findById(copy.getId()).orElseThrow().getStatus());
        mvc.perform(get("/circulation"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Loan history")));
        mvc.perform(post("/circulation/" + loan.getId() + "/delete").with(csrf()))
                .andExpect(flash().attributeExists("success"));
        assertFalse(loans.existsById(loan.getId()));
        assertTrue(users.existsById(member.getId()));
        assertTrue(copies.existsById(copy.getId()));
        assertTrue(audit.findAll().stream().anyMatch(a -> "DELETE".equals(a.getAction())
                && loan.getId().equals(a.getEntityId())));
    }

    @Test
    void memberRequestsStillReachDeskAndCanBeIssued() throws Exception {
        mvc.perform(post("/my-library/borrow/" + copy.getBook().getId())
                .with(user(member.getEmail()).roles("MEMBER")).with(csrf()))
                .andExpect(redirectedUrl("/my-library"));
        Loan request = loans.findByMemberIdOrderByRequestedAtDesc(member.getId()).get(0);
        assertEquals(LoanStatus.REQUESTED, request.getStatus());
        mvc.perform(post("/circulation/" + request.getId() + "/issue").with(csrf()))
                .andExpect(flash().attributeExists("success"));
        assertEquals(LoanStatus.ACTIVE, request.getStatus());
        assertEquals(BookCopyStatus.LOANED, copy.getStatus());
    }

    @Test
    void deletingPendingRequestReleasesReservedCopy() throws Exception {
        Loan request = pending();
        mvc.perform(post("/circulation/" + request.getId() + "/delete").with(csrf()))
                .andExpect(flash().attributeExists("success"));
        assertFalse(loans.existsById(request.getId()));
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void declinePreservesInventoryDamageFlag() throws Exception {
        Loan request = pending();
        copy.setStatus(BookCopyStatus.DAMAGED);
        copies.saveAndFlush(copy);
        mvc.perform(post("/circulation/" + request.getId() + "/reject").with(csrf()))
                .andExpect(flash().attributeExists("success"));
        assertEquals(LoanStatus.REJECTED, request.getStatus());
        assertEquals(BookCopyStatus.DAMAGED, copy.getStatus());
    }

    @Test
    void duplicateCreateDoesNotCreateAnotherLoan() throws Exception {
        create();
        long before = loans.count();
        mvc.perform(post("/circulation/create").with(csrf()).param("memberId", member.getId().toString())
                .param("copyId", copy.getId().toString())).andExpect(flash().attributeExists("error"));
        assertEquals(before, loans.count());
    }

    @Test
    void inactiveMemberCannotReceiveLoan() throws Exception {
        member.setActive(false);
        users.saveAndFlush(member);
        rejectedCreate(copy);
        assertTrue(loans.findByMemberIdOrderByRequestedAtDesc(member.getId()).isEmpty());
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void staffAccountCannotBeUsedAsMember() throws Exception {
        member.setRole(Role.CIRCULATION_STAFF);
        users.saveAndFlush(member);
        rejectedCreate(copy);
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void inactiveBookCannotBeIssued() throws Exception {
        copy.getBook().setActive(false);
        books.saveAndFlush(copy.getBook());
        rejectedCreate(copy);
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void fourthOpenLoanIsBlocked() throws Exception {
        for (int i = 2; i <= 4; i++) {
            BookCopy other = newCopy("CIRC-ISBN-" + i, "CIRC-COPY-" + i);
            loans.saveAndFlush(new Loan(member, other));
        }
        rejectedCreate(copy);
        assertEquals(3, loans.findByMemberIdOrderByRequestedAtDesc(member.getId()).size());
    }

    @Test
    void thirdOpenLoanIsAllowed() throws Exception {
        for (int i = 2; i <= 3; i++) {
            loans.saveAndFlush(new Loan(member, newCopy("CIRC-ISBN-" + i, "CIRC-COPY-" + i)));
        }
        assertEquals(LoanStatus.ACTIVE, create().getStatus());
        assertEquals(3, loans.findByMemberIdOrderByRequestedAtDesc(member.getId()).size());
    }

    @Test
    void overdueBookBlocksNewIssue() throws Exception {
        BookCopy other = newCopy("CIRC-ISBN-2", "CIRC-COPY-2");
        Loan overdue = new Loan(member, other);
        overdue.setStatus(LoanStatus.ACTIVE);
        overdue.setIssuedAt(LocalDateTime.now().minusDays(20));
        overdue.setDueAt(LocalDateTime.now().minusDays(6));
        loans.saveAndFlush(overdue);
        rejectedCreate(copy);
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void secondCopyOfSameTitleIsBlocked() throws Exception {
        create();
        BookCopy second = copies.saveAndFlush(new BookCopy(copy.getBook(), "CIRC-COPY-2", "TEST"));
        rejectedCreate(second);
        assertEquals(BookCopyStatus.AVAILABLE, second.getStatus());
    }

    @Test
    void activeLoanCannotBeDeletedOrDeclined() throws Exception {
        Loan loan = create();
        mvc.perform(post("/circulation/" + loan.getId() + "/delete").with(csrf()))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(post("/circulation/" + loan.getId() + "/reject").with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(BookCopyStatus.LOANED, copy.getStatus());
    }

    @Test
    void invalidDueDateDoesNotChangeLoan() throws Exception {
        Loan loan = create();
        LocalDateTime before = loan.getDueAt();
        mvc.perform(post("/circulation/" + loan.getId() + "/update").with(csrf())
                .param("dueAt", loan.getIssuedAt().minusDays(1).toString()))
                .andExpect(flash().attribute("error", containsString("after the issue date")));
        assertEquals(before.truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                loan.getDueAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    void renewOnlyOnceForSevenDays() throws Exception {
        Loan loan = create();
        LocalDateTime before = loan.getDueAt();
        mvc.perform(post("/circulation/" + loan.getId() + "/renew").with(csrf()))
                .andExpect(flash().attributeExists("success"));
        assertEquals(before.plusDays(7).truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                loan.getDueAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertEquals(1, loan.getRenewalCount());
        // MockMvc joins this test's transaction; flush to model the commit between real HTTP requests.
        loans.flush();
        mvc.perform(post("/circulation/" + loan.getId() + "/renew").with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertEquals(before.plusDays(7).truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                loan.getDueAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    void lateReturnCreatesOneFineAndLinkedLoanCannotBeDeleted() throws Exception {
        Loan loan = create();
        loan.setIssuedAt(LocalDateTime.now().minusDays(20));
        loan.setDueAt(LocalDateTime.now().minusDays(3));
        loans.saveAndFlush(loan);
        mvc.perform(post("/circulation/" + loan.getId() + "/return").with(csrf()))
                .andExpect(flash().attributeExists("success"));
        FinePayment fine = fines.findByMemberIdOrderByIdDesc(member.getId()).get(0);
        assertEquals(0, BigDecimal.valueOf(75).compareTo(fine.getAmount()));
        long count = fines.count();
        mvc.perform(post("/circulation/" + loan.getId() + "/return").with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertEquals(count, fines.count());
        mvc.perform(post("/circulation/" + loan.getId() + "/delete").with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertTrue(loans.existsById(loan.getId()));
    }

    @Test
    void unpaidFineBlocksNewLoan() throws Exception {
        Loan old = loans.saveAndFlush(new Loan(member, newCopy("CIRC-ISBN-2", "CIRC-COPY-2")));
        old.setStatus(LoanStatus.RETURNED);
        loans.saveAndFlush(old);
        FinePayment fine = new FinePayment();
        fine.setLoan(old);
        fine.setMember(member);
        fine.setAmount(BigDecimal.valueOf(25));
        fines.saveAndFlush(fine);
        rejectedCreate(copy);
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void malformedOrMissingInputShowsError() throws Exception {
        mvc.perform(post("/circulation/create").with(csrf()).param("memberId", "bad").param("copyId", "bad"))
                .andExpect(redirectedUrl("/circulation")).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/circulation/create").with(csrf()))
                .andExpect(redirectedUrl("/circulation")).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/circulation/999999999/return").with(csrf()))
                .andExpect(redirectedUrl("/circulation")).andExpect(flash().attributeExists("error"));
    }

    @Test
    void mutationsRequireCsrfAndCorrectRole() throws Exception {
        mvc.perform(post("/circulation/create").param("memberId", member.getId().toString())
                .param("copyId", copy.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/circulation/create").with(csrf()).with(user(member.getEmail()).roles("MEMBER"))
                .param("memberId", member.getId().toString()).param("copyId", copy.getId().toString()))
                .andExpect(status().isForbidden());
    }

    private Loan create() throws Exception {
        mvc.perform(post("/circulation/create").with(csrf()).param("memberId", member.getId().toString())
                .param("copyId", copy.getId().toString())).andExpect(redirectedUrl("/circulation"))
                .andExpect(flash().attributeExists("success"));
        return loans.findByMemberIdOrderByRequestedAtDesc(member.getId()).get(0);
    }

    private void rejectedCreate(BookCopy selected) throws Exception {
        mvc.perform(post("/circulation/create").with(csrf()).param("memberId", member.getId().toString())
                .param("copyId", selected.getId().toString())).andExpect(flash().attributeExists("error"));
    }

    private Loan pending() {
        copy.setStatus(BookCopyStatus.RESERVED);
        copies.saveAndFlush(copy);
        return loans.saveAndFlush(new Loan(member, copy));
    }

    private BookCopy newCopy(String isbn, String barcode) {
        Book book = books.saveAndFlush(new Book(isbn, "Circulation test " + isbn, "Test author", "Test"));
        return copies.saveAndFlush(new BookCopy(book, barcode, "TEST"));
    }
}
