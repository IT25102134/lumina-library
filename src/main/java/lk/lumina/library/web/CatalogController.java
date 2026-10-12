package lk.lumina.library.web;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import lk.lumina.library.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogController {
    // Local destination for validated cover-image uploads; UploadConfig exposes the directory as /uploads/**.
    private static final Path UPLOADS = Path.of("uploads").toAbsolutePath().normalize();
    // Repositories supply book/copy data, while the services identify the actor and record management actions.
    private final BookRepository books; private final BookCopyRepository copies; private final AuditService audit; private final CurrentUserService current;
    // Constructor injection gives the controller its catalogue persistence, availability, audit, and current-user collaborators.
    public CatalogController(BookRepository books,BookCopyRepository copies,AuditService audit,CurrentUserService current){this.books=books;this.copies=copies;this.audit=audit;this.current=current;}
    // Displays active catalogue records; the optional q parameter enables the search box.
    @GetMapping("/catalog") String catalog(@RequestParam(defaultValue="") String q,Model model){
        // A blank search shows all active titles; otherwise the repository performs a trimmed multi-field search.
        List<Book> result=q.isBlank()?books.findByActiveTrueOrderByTitleAsc():books.search(q.trim());
        // Maps each Book ID to the number shown in that catalogue card's availability badge.
        Map<Long,Long> availability=new HashMap<>();
        // Only physical copies currently marked AVAILABLE contribute to the displayed count.
        result.forEach(b->availability.put(b.getId(),copies.findByBookId(b.getId()).stream().filter(c->c.getStatus()==BookCopyStatus.AVAILABLE).count()));
        // Thymeleaf receives the cards, availability counts, and original query before rendering catalog.html.
        model.addAttribute("books",result);model.addAttribute("availability",availability);model.addAttribute("q",q);return "catalog";
    }
    // Opens a blank catalogue form; method security limits this management action to Head Librarians.
    @PreAuthorize("hasRole('HEAD_LIBRARIAN')") @GetMapping("/catalog/manage/new")
    String create(Model model){model.addAttribute("book",new Book());return "book-form";}
    // Loads an existing Book into the same form so the Head Librarian can edit it.
    @PreAuthorize("hasRole('HEAD_LIBRARIAN')") @GetMapping("/catalog/manage/{id}/edit")
    String edit(@PathVariable Long id,Model model){model.addAttribute("book",books.findById(id).orElseThrow());return "book-form";}
    // Handles both create and update submissions, including the optional cover-image upload.
    @PreAuthorize("hasRole('HEAD_LIBRARIAN')") @PostMapping("/catalog/manage/save")
    String save(@ModelAttribute Book input,@RequestParam(required=false) MultipartFile coverImage,RedirectAttributes flash){
        // A missing ID means create; an existing ID reloads the persistent entity so identity and creation time are preserved.
        Book b=input.getId()==null?new Book():books.findById(input.getId()).orElseThrow();
        // Copies the bibliographic form fields into the entity that will be persisted.
        b.setIsbn(input.getIsbn());b.setTitle(input.getTitle());b.setAuthor(input.getAuthor());b.setCategory(input.getCategory());
        b.setPublisher(input.getPublisher());b.setPublicationYear(input.getPublicationYear());b.setDescription(input.getDescription());
        // Starts with the optional external cover URL; a valid uploaded file takes priority below.
        String coverUrl=input.getCoverUrl();
        // Processes a local cover only when the user selected a non-empty file.
        if(coverImage!=null&&!coverImage.isEmpty()){
            // Reads both MIME type and extension because either value alone can be misleading.
            String type=coverImage.getContentType();
            String name=Optional.ofNullable(coverImage.getOriginalFilename()).orElse("");
            String extension=name.contains(".")?name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT):"";
            // Rejects non-images, unsupported extensions, and files larger than 5 MB before anything is written to disk.
            if(type==null||!type.startsWith("image/")||!Set.of(".jpg",".jpeg",".png",".gif",".webp").contains(extension)||coverImage.getSize()>5*1024*1024){flash.addFlashAttribute("error","Choose a JPG, PNG, GIF, or WebP image up to 5 MB.");return "redirect:/catalog/manage/"+(input.getId()==null?"new":input.getId()+"/edit");}
            // A UUID filename avoids collisions between uploads that originally had the same name.
            String stored=UUID.randomUUID()+extension;
            // Creates the upload directory when needed, stores the file, and converts its path into a browser-facing URL.
            try{Files.createDirectories(UPLOADS);Files.copy(coverImage.getInputStream(),UPLOADS.resolve(stored),StandardCopyOption.REPLACE_EXISTING);coverUrl="/uploads/"+stored;}catch(IOException e){flash.addFlashAttribute("error","Could not save the cover image.");return "redirect:/catalog/manage/"+(input.getId()==null?"new":input.getId()+"/edit");}
        }
        // Saves links and reactivates the record, then persists the completed catalogue entity.
        b.setCoverUrl(coverUrl);b.setEbookUrl(input.getEbookUrl());b.setActive(true);books.save(b);
        // Records who created or updated the book for the audit trail.
        audit.record(current.get().getEmail(),input.getId()==null?"CREATE":"UPDATE","Book",b.getId(),b.getTitle());
        // Uses a flash message after redirect so refresh does not resubmit the form.
        flash.addFlashAttribute("success","Catalogue record saved.");return "redirect:/catalog";
    }
    // Soft-deletes a record for a Head Librarian instead of removing its database row.
    @PreAuthorize("hasRole('HEAD_LIBRARIAN')") @PostMapping("/catalog/manage/{id}/delete")
    String delete(@PathVariable Long id,RedirectAttributes flash){Book b=books.findById(id).orElseThrow();b.setActive(false);books.save(b);audit.record(current.get().getEmail(),"DELETE","Book",id,b.getTitle());flash.addFlashAttribute("success","Book deleted.");return "redirect:/catalog";}
}
