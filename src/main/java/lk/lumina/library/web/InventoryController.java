package lk.lumina.library.web;

import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/inventory") @PreAuthorize("hasRole('LIBRARY_ASSISTANT')")
public class InventoryController {
    private final BookCopyRepository copies;private final BookRepository books;private final AuditService audit;private final CurrentUserService current;
    public InventoryController(BookCopyRepository copies,BookRepository books,AuditService audit,CurrentUserService current){this.copies=copies;this.books=books;this.audit=audit;this.current=current;}
    @GetMapping String page(Model model){model.addAttribute("copies",copies.findAllByOrderByBarcodeAsc());model.addAttribute("books",books.findByActiveTrueOrderByTitleAsc());model.addAttribute("statuses",BookCopyStatus.values());return "inventory";}
    @PostMapping String add(@RequestParam Long bookId,@RequestParam String barcode,@RequestParam String shelfLocation,RedirectAttributes flash){
        BookCopy copy=copies.save(new BookCopy(books.findById(bookId).orElseThrow(),barcode.trim(),shelfLocation.trim()));audit.record(current.get().getEmail(),"CREATE","BookCopy",copy.getId(),barcode);flash.addFlashAttribute("success","Physical copy added to inventory.");return "redirect:/inventory";
    }
    @PostMapping("/{id}/status") String status(@PathVariable Long id,@RequestParam BookCopyStatus status,@RequestParam String shelfLocation,RedirectAttributes flash){
        BookCopy copy=copies.findById(id).orElseThrow();copy.setStatus(status);copy.setShelfLocation(shelfLocation);copies.save(copy);audit.record(current.get().getEmail(),"UPDATE_STATUS","BookCopy",id,status.name());flash.addFlashAttribute("success","Inventory record updated.");return "redirect:/inventory";
    }
}
