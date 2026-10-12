package lk.lumina.library.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import lk.lumina.library.model.Book;
import lk.lumina.library.model.BookCopy;
import lk.lumina.library.model.BookCopyStatus;
import lk.lumina.library.model.Loan;
import lk.lumina.library.model.UserAccount;
import lk.lumina.library.repository.BookCopyRepository;
import lk.lumina.library.repository.BookRepository;
import lk.lumina.library.repository.LoanRepository;
import lk.lumina.library.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BookCopyRepository copies;

    @Autowired
    private BookRepository books;

    @Autowired
    private LoanRepository loans;

    @Autowired
    private UserAccountRepository users;


    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void validPhysicalCopyCreationAndPersistence() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);
        String uniqueBarcode = "TEST-BARCODE-" + System.currentTimeMillis();

        mvc.perform(post("/inventory")
                        .with(csrf())
                        .param("bookId", book.getId().toString())
                        .param("barcode", uniqueBarcode)
                        .param("shelfLocation", "TEST-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attributeExists("success"));

        // Persistence verification
        assertTrue(copies.existsByBarcodeIgnoreCase(uniqueBarcode));

        // Page reload verification
        mvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(uniqueBarcode)));
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void duplicateBarcodeRejection() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);
        String existingBarcode = "LUM-0001"; // Seeded barcode

        mvc.perform(post("/inventory")
                        .with(csrf())
                        .param("bookId", book.getId().toString())
                        .param("barcode", existingBarcode)
                        .param("shelfLocation", "SHELF-X"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void blankBarcodeAndShelfLocationRejection() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);

        // Blank barcode
        mvc.perform(post("/inventory")
                        .with(csrf())
                        .param("bookId", book.getId().toString())
                        .param("barcode", "   ")
                        .param("shelfLocation", "A-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("Barcode is required")));

        // Blank shelf
        mvc.perform(post("/inventory")
                        .with(csrf())
                        .param("bookId", book.getId().toString())
                        .param("barcode", "VALID-BARCODE-99")
                        .param("shelfLocation", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("Shelf location is required")));
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void invalidCatalogueReferenceRejection() throws Exception {
        mvc.perform(post("/inventory")
                        .with(csrf())
                        .param("bookId", "9999999")
                        .param("barcode", "SOME-BARCODE-1")
                        .param("shelfLocation", "A-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("does not exist or is inactive")));
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void validShelfAndStatusUpdates() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);
        String barcode = "UPDATE-TEST-" + System.currentTimeMillis();
        BookCopy copy = copies.save(new BookCopy(book, barcode, "OLD-SHELF"));

        mvc.perform(post("/inventory/" + copy.getId() + "/status")
                        .with(csrf())
                        .param("status", "DAMAGED")
                        .param("shelfLocation", "NEW-SHELF-99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attributeExists("success"));

        BookCopy reloaded = copies.findById(copy.getId()).orElseThrow();
        assertEquals(BookCopyStatus.DAMAGED, reloaded.getStatus());
        assertEquals("NEW-SHELF-99", reloaded.getShelfLocation());
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void protectionAgainstAssigningCirculationStatuses() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);
        String barcode = "PROTECT-TEST-" + System.currentTimeMillis();
        BookCopy copy = copies.save(new BookCopy(book, barcode, "SHELF-P"));

        // Reject assigning LOANED
        mvc.perform(post("/inventory/" + copy.getId() + "/status")
                        .with(csrf())
                        .param("status", "LOANED")
                        .param("shelfLocation", "SHELF-P"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("Circulation-managed statuses (LOANED) cannot be assigned")));

        // Reject assigning RESERVED
        mvc.perform(post("/inventory/" + copy.getId() + "/status")
                        .with(csrf())
                        .param("status", "RESERVED")
                        .param("shelfLocation", "SHELF-P"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("Circulation-managed statuses (RESERVED) cannot be assigned")));

        BookCopy untouched = copies.findById(copy.getId()).orElseThrow();
        assertEquals(BookCopyStatus.AVAILABLE, untouched.getStatus());
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void protectionAgainstAlteringCirculationManagedCopies() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);
        String barcode = "LOANED-TEST-" + System.currentTimeMillis();
        BookCopy copy = new BookCopy(book, barcode, "SHELF-L");
        copy.setStatus(BookCopyStatus.LOANED);
        copy = copies.save(copy);

        // Attempt to change status to AVAILABLE from Inventory
        mvc.perform(post("/inventory/" + copy.getId() + "/status")
                        .with(csrf())
                        .param("status", "AVAILABLE")
                        .param("shelfLocation", "SHELF-L-NEW"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("managed by Circulation")));

        BookCopy untouched = copies.findById(copy.getId()).orElseThrow();
        assertEquals(BookCopyStatus.LOANED, untouched.getStatus());
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void searchAndFilterBehaviour() throws Exception {
        // Search by barcode
        mvc.perform(get("/inventory").param("q", "LUM-0001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LUM-0001")));

        // Search by title
        mvc.perform(get("/inventory").param("q", "Cities"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A Tale of Two Cities")));

        // Filter by status AVAILABLE
        mvc.perform(get("/inventory").param("status", "AVAILABLE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void safeDeleteOfUnlinkedPhysicalCopy() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);
        String barcode = "DEL-SAFE-" + System.currentTimeMillis();
        BookCopy copy = copies.save(new BookCopy(book, barcode, "SHELF-TEMP"));

        mvc.perform(post("/inventory/" + copy.getId() + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("success", containsString("deleted successfully")));

        assertTrue(copies.findById(copy.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void rejectionOfDeletingCopyLinkedToLoanHistory() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);
        String barcode = "DEL-LOAN-" + System.currentTimeMillis();
        BookCopy copy = copies.save(new BookCopy(book, barcode, "SHELF-LOAN"));

        UserAccount member = users.findAll().stream()
                .filter(u -> u.getRole().name().equals("MEMBER"))
                .findFirst().orElseThrow();

        loans.save(new Loan(member, copy));

        mvc.perform(post("/inventory/" + copy.getId() + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("circulation/loan history")));

        assertTrue(copies.findById(copy.getId()).isPresent());

    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void rejectionOfDeletingCirculationManagedCopy() throws Exception {
        Book book = books.findByActiveTrueOrderByTitleAsc().get(0);
        String barcode = "DEL-STATUS-" + System.currentTimeMillis();
        BookCopy copy = new BookCopy(book, barcode, "SHELF-MAN");
        copy.setStatus(BookCopyStatus.LOANED);
        copy = copies.save(copy);

        mvc.perform(post("/inventory/" + copy.getId() + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("under Circulation")));

        assertTrue(copies.findById(copy.getId()).isPresent());
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void rejectionOfDeletingNonExistentCopy() throws Exception {
        mvc.perform(post("/inventory/99999999/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("error", containsString("not found")));
    }

    @Test
    @WithMockUser(username = "inventory@lumina.lk", roles = "LIBRARY_ASSISTANT")
    void newCatalogueBookCreationAndSelectionInInventory() throws Exception {
        // 1. Verify Library Assistant can access /catalog/manage/new
        mvc.perform(get("/catalog/manage/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bibliographic details")));

        // 2. Create a new book title through the existing Catalogue save endpoint
        String uniqueIsbn = "978" + (System.currentTimeMillis() % 10000000000L);
        String title = "New Inventory Test Title " + System.currentTimeMillis();

        mvc.perform(post("/catalog/manage/save")
                        .with(csrf())
                        .param("isbn", uniqueIsbn)
                        .param("title", title)
                        .param("author", "Test Author")
                        .param("category", "Science")
                        .param("publisher", "Test Press")
                        .param("publicationYear", "2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalog"))
                .andExpect(flash().attribute("success", containsString("saved")));

        // 3. Verify newly created book appears in the Inventory page dropdown
        mvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(title)))
                .andExpect(content().string(containsString(uniqueIsbn)));

        // 4. Verify physical copy can now be created for this new catalogue book
        Book savedBook = books.findByActiveTrueOrderByTitleAsc().stream()
                .filter(b -> b.getIsbn().equals(uniqueIsbn))
                .findFirst().orElseThrow();

        String barcode = "NEW-BOOK-COPY-" + System.currentTimeMillis();
        mvc.perform(post("/inventory")
                        .with(csrf())
                        .param("bookId", savedBook.getId().toString())
                        .param("barcode", barcode)
                        .param("shelfLocation", "D-05-12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inventory"))
                .andExpect(flash().attribute("success", containsString("added to inventory")));

        assertTrue(copies.existsByBarcodeIgnoreCase(barcode));
    }
}


