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
    private static final Path UPLOADS = Path.of("uploads").toAbsolutePath().normalize();
    private final BookRepository books; private final BookCopyRepository copies; private final AuditService audit; private final CurrentUserService current;
    public CatalogController(BookRepository books,BookCopyRepository copies,AuditService audit,CurrentUserService current){this.books=books;this.copies=copies;this.audit=audit;this.current=current;}
    @GetMapping("/catalog") String catalog(@RequestParam(defaultValue="") String q,Model model){
        List<Book> result=q.isBlank()?books.findByActiveTrueOrderByTitleAsc():books.search(q.trim());
        Map<Long,Long> availability=new HashMap<>();
        result.forEach(b->availability.put(b.getId(),copies.findByBookId(b.getId()).stream().filter(c->c.getStatus()==BookCopyStatus.AVAILABLE).count()));
        model.addAttribute("books",result);model.addAttribute("availability",availability);model.addAttribute("q",q);return "catalog";
    }
    @PreAuthorize("hasRole('HEAD_LIBRARIAN')") @GetMapping("/catalog/manage/new")
    String create(Model model){model.addAttribute("book",new Book());return "book-form";}
    @PreAuthorize("hasRole('HEAD_LIBRARIAN')") @GetMapping("/catalog/manage/{id}/edit")
    String edit(@PathVariable Long id,Model model){model.addAttribute("book",books.findById(id).orElseThrow());return "book-form";}
    @PreAuthorize("hasRole('HEAD_LIBRARIAN')") @PostMapping("/catalog/manage/save")
    String save(@ModelAttribute Book input,@RequestParam(required=false) MultipartFile coverImage,RedirectAttributes flash){
        Book b=input.getId()==null?new Book():books.findById(input.getId()).orElseThrow();
        b.setIsbn(input.getIsbn());b.setTitle(input.getTitle());b.setAuthor(input.getAuthor());b.setCategory(input.getCategory());
        b.setPublisher(input.getPublisher());b.setPublicationYear(input.getPublicationYear());b.setDescription(input.getDescription());
        String coverUrl=input.getCoverUrl();
        if(coverImage!=null&&!coverImage.isEmpty()){
            String type=coverImage.getContentType();
            String name=Optional.ofNullable(coverImage.getOriginalFilename()).orElse("");
            String extension=name.contains(".")?name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT):"";
            if(type==null||!type.startsWith("image/")||!Set.of(".jpg",".jpeg",".png",".gif",".webp").contains(extension)||coverImage.getSize()>5*1024*1024){flash.addFlashAttribute("error","Choose a JPG, PNG, GIF, or WebP image up to 5 MB.");return "redirect:/catalog/manage/"+(input.getId()==null?"new":input.getId()+"/edit");}
            String stored=UUID.randomUUID()+extension;
            try{Files.createDirectories(UPLOADS);Files.copy(coverImage.getInputStream(),UPLOADS.resolve(stored),StandardCopyOption.REPLACE_EXISTING);coverUrl="/uploads/"+stored;}catch(IOException e){flash.addFlashAttribute("error","Could not save the cover image.");return "redirect:/catalog/manage/"+(input.getId()==null?"new":input.getId()+"/edit");}
        }
        b.setCoverUrl(coverUrl);b.setEbookUrl(input.getEbookUrl());b.setActive(true);books.save(b);
        audit.record(current.get().getEmail(),input.getId()==null?"CREATE":"UPDATE","Book",b.getId(),b.getTitle());
        flash.addFlashAttribute("success","Catalogue record saved.");return "redirect:/catalog";
    }
    @PreAuthorize("hasRole('HEAD_LIBRARIAN')") @PostMapping("/catalog/manage/{id}/delete")
    String delete(@PathVariable Long id,RedirectAttributes flash){Book b=books.findById(id).orElseThrow();b.setActive(false);books.save(b);audit.record(current.get().getEmail(),"DELETE","Book",id,b.getTitle());flash.addFlashAttribute("success","Book deleted.");return "redirect:/catalog";}
}
