package lk.lumina.library.web;

import java.util.List;
import java.util.Optional;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
@PreAuthorize("hasRole('LIBRARY_ASSISTANT')")
public class InventoryController {

    private static final List<BookCopyStatus> ASSIGNABLE_STATUSES = List.of(
            BookCopyStatus.AVAILABLE,
            BookCopyStatus.DAMAGED,
            BookCopyStatus.LOST
    );

    private final BookCopyRepository copies;
    private final BookRepository books;
    private final AuditService audit;
    private final CurrentUserService current;

    public InventoryController(BookCopyRepository copies, BookRepository books, AuditService audit, CurrentUserService current) {
        this.copies = copies;
        this.books = books;
        this.audit = audit;
        this.current = current;
    }

    @GetMapping
    public String page(@RequestParam(required = false) String q,
                       @RequestParam(required = false) BookCopyStatus status,
                       Model model) {
        boolean hasQuery = q != null && !q.trim().isEmpty();
        boolean hasStatus = status != null;
        List<BookCopy> copyList;

        if (hasQuery && hasStatus) {
            copyList = copies.searchByQueryAndStatus(q.trim(), status);
        } else if (hasQuery) {
            copyList = copies.searchByQuery(q.trim());
        } else if (hasStatus) {
            copyList = copies.findByStatusOrderByBarcodeAsc(status);
        } else {
            copyList = copies.findAllByOrderByBarcodeAsc();
        }

        model.addAttribute("copies", copyList);
        model.addAttribute("books", books.findByActiveTrueOrderByTitleAsc());
        model.addAttribute("statuses", BookCopyStatus.values());
        model.addAttribute("assignableStatuses", ASSIGNABLE_STATUSES);
        model.addAttribute("q", q != null ? q.trim() : "");
        model.addAttribute("selectedStatus", status != null ? status.name() : "");
        return "inventory";
    }

    @PostMapping
    public String add(@RequestParam(required = false) Long bookId,
                      @RequestParam(required = false) String barcode,
                      @RequestParam(required = false) String shelfLocation,
                      RedirectAttributes flash) {
        if (bookId == null) {
            flash.addFlashAttribute("error", "Please select a catalogue title.");
            return "redirect:/inventory";
        }

        if (barcode == null || barcode.trim().isEmpty()) {
            flash.addFlashAttribute("error", "Barcode is required and cannot be blank.");
            return "redirect:/inventory";
        }

        if (shelfLocation == null || shelfLocation.trim().isEmpty()) {
            flash.addFlashAttribute("error", "Shelf location is required and cannot be blank.");
            return "redirect:/inventory";
        }

        String cleanBarcode = barcode.trim();
        String cleanShelf = shelfLocation.trim();

        Optional<Book> bookOpt = books.findById(bookId);
        if (bookOpt.isEmpty() || !bookOpt.get().isActive()) {
            flash.addFlashAttribute("error", "The selected catalogue book does not exist or is inactive.");
            return "redirect:/inventory";
        }

        if (copies.existsByBarcodeIgnoreCase(cleanBarcode)) {
            flash.addFlashAttribute("error", "A physical copy with barcode '" + cleanBarcode + "' already exists.");
            return "redirect:/inventory";
        }

        try {
            BookCopy copy = copies.save(new BookCopy(bookOpt.get(), cleanBarcode, cleanShelf));
            audit.record(current.get().getEmail(), "CREATE", "BookCopy", copy.getId(), cleanBarcode);
            flash.addFlashAttribute("success", "Physical copy " + cleanBarcode + " added to inventory.");
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "Could not save copy: barcode '" + cleanBarcode + "' violates database uniqueness.");
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "An unexpected error occurred while saving the physical copy.");
        }

        return "redirect:/inventory";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id,
                         @RequestParam(required = false) BookCopyStatus status,
                         @RequestParam(required = false) String shelfLocation,
                         RedirectAttributes flash) {
        Optional<BookCopy> copyOpt = copies.findById(id);
        if (copyOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Physical copy not found.");
            return "redirect:/inventory";
        }

        BookCopy copy = copyOpt.get();

        if (shelfLocation == null || shelfLocation.trim().isEmpty()) {
            flash.addFlashAttribute("error", "Shelf location is required and cannot be blank.");
            return "redirect:/inventory";
        }

        if (status == null) {
            flash.addFlashAttribute("error", "A valid status must be selected.");
            return "redirect:/inventory";
        }

        // Protection: Circulation-managed statuses cannot be assigned manually from Inventory
        if (status == BookCopyStatus.LOANED || status == BookCopyStatus.RESERVED) {
            flash.addFlashAttribute("error", "Circulation-managed statuses (" + status + ") cannot be assigned manually from Inventory.");
            return "redirect:/inventory";
        }

        // Protection: Copies currently managed by Circulation (active loan or reservation) cannot have status modified here
        if (copy.getStatus() == BookCopyStatus.LOANED || copy.getStatus() == BookCopyStatus.RESERVED) {
            flash.addFlashAttribute("error", "Copy " + copy.getBarcode() + " is currently " + copy.getStatus() + " and managed by Circulation. Its status cannot be altered here.");
            return "redirect:/inventory";
        }

        try {
            String cleanShelf = shelfLocation.trim();
            copy.setStatus(status);
            copy.setShelfLocation(cleanShelf);
            copies.save(copy);
            audit.record(current.get().getEmail(), "UPDATE_STATUS", "BookCopy", id, status.name() + " @ " + cleanShelf);
            flash.addFlashAttribute("success", "Inventory record updated for " + copy.getBarcode() + ".");
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "Failed to update inventory record: " + ex.getMessage());
        }

        return "redirect:/inventory";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        Optional<BookCopy> copyOpt = copies.findById(id);
        if (copyOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Physical copy not found.");
            return "redirect:/inventory";
        }

        BookCopy copy = copyOpt.get();

        // 1. Safety check: Copy currently in active circulation status cannot be deleted
        if (copy.getStatus() == BookCopyStatus.LOANED || copy.getStatus() == BookCopyStatus.RESERVED) {
            flash.addFlashAttribute("error", "Cannot delete copy " + copy.getBarcode() + " because it is currently " + copy.getStatus() + " under Circulation.");
            return "redirect:/inventory";
        }

        // 2. Safety check: Copy with any loan or circulation history cannot be deleted
        long loanCount = copies.countLoansByCopyId(copy.getId());
        if (loanCount > 0) {
            flash.addFlashAttribute("error", "Cannot delete copy " + copy.getBarcode() + " because it has existing circulation/loan history (" + loanCount + " record(s)).");
            return "redirect:/inventory";
        }

        // 3. Perform safe deletion
        try {
            String barcode = copy.getBarcode();
            copies.delete(copy);
            audit.record(current.get().getEmail(), "DELETE", "BookCopy", id, barcode);
            flash.addFlashAttribute("success", "Physical copy " + barcode + " was deleted successfully.");
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "Cannot delete copy " + copy.getBarcode() + " due to active database constraints or references.");
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "An error occurred while deleting the physical copy: " + ex.getMessage());
        }

        return "redirect:/inventory";
    }
}


