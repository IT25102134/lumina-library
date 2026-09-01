package lk.lumina.library.config;

import java.time.LocalDateTime;
import java.util.List;
import lk.lumina.library.model.*;
import lk.lumina.library.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class SeedData {
    @Bean @Transactional
    CommandLineRunner seed(UserAccountRepository users,BookRepository books,BookCopyRepository copies,
        LibraryEventRepository events,PasswordEncoder encoder,NotificationRepository notifications){
        return args -> {
            if(users.count()>0) return;
            String password=encoder.encode("Library@123");
            List<UserAccount> staff=List.of(
                account("Amara","Perera","head@lumina.lk",password,Role.HEAD_LIBRARIAN,"STAFF-001"),
                account("Nimal","Fernando","circulation@lumina.lk",password,Role.CIRCULATION_STAFF,"STAFF-002"),
                account("Maya","Silva","member@lumina.lk",password,Role.MEMBER,"MEM-2026-001"),
                account("Ravi","Jayasinghe","inventory@lumina.lk",password,Role.LIBRARY_ASSISTANT,"STAFF-004"),
                account("Zara","Imran","events@lumina.lk",password,Role.EVENT_COORDINATOR,"STAFF-005"),
                account("Dinuka","Senanayake","manager@lumina.lk",password,Role.LIBRARY_MANAGER,"STAFF-006"));
            users.saveAll(staff);

            Book b1=book("9780141439600","A Tale of Two Cities","Charles Dickens","Classics","Penguin Classics",1859,"A sweeping story of sacrifice and renewal across London and Paris.","https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=700&q=80");
            Book b2=book("9780061120084","To Kill a Mockingbird","Harper Lee","Literary Fiction","Harper Perennial",1960,"A timeless exploration of courage, justice and compassion.","https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=700&q=80");
            Book b3=book("9780553380163","A Brief History of Time","Stephen Hawking","Science","Bantam",1988,"An accessible journey through space, time and the universe.","https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=700&q=80");
            Book b4=book("9781612681139","The Midnight Library","Matt Haig","Contemporary Fiction","Canongate",2020,"A hopeful story about possibility, regret and the lives we choose.","https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=700&q=80");
            books.saveAll(List.of(b1,b2,b3,b4));
            copies.saveAll(List.of(
                new BookCopy(b1,"LUM-0001","A-01-01"),new BookCopy(b1,"LUM-0002","A-01-01"),
                new BookCopy(b2,"LUM-0003","B-02-04"),new BookCopy(b3,"LUM-0004","C-04-02"),
                new BookCopy(b4,"LUM-0005","B-01-07"),new BookCopy(b4,"LUM-0006","B-01-07")));

            LibraryEvent e1=new LibraryEvent(); e1.setTitle("Twilight Readers' Circle"); e1.setDescription("A relaxed monthly conversation about contemporary fiction, with tea and new perspectives."); e1.setLocation("Orchid Reading Hall"); e1.setStartAt(LocalDateTime.now().plusDays(8).withHour(18).withMinute(0)); e1.setCapacity(36);
            LibraryEvent e2=new LibraryEvent(); e2.setTitle("Young Inventors Lab"); e2.setDescription("A hands-on science and storytelling workshop for curious young minds."); e2.setLocation("Discovery Studio"); e2.setStartAt(LocalDateTime.now().plusDays(15).withHour(10).withMinute(30)); e2.setCapacity(24);
            events.saveAll(List.of(e1,e2));
            notifications.save(new Notification(staff.get(2),"Welcome to Lumina","Your digital library card is ready. Explore the catalogue and upcoming programs.",NotificationType.SUCCESS));
        };
    }
    private UserAccount account(String f,String l,String e,String p,Role r,String number){UserAccount u=new UserAccount(f,l,e,p,r);u.setMembershipNumber(number);return u;}
    private Book book(String isbn,String title,String author,String category,String publisher,int year,String description,String cover){Book b=new Book(isbn,title,author,category);b.setPublisher(publisher);b.setPublicationYear(year);b.setDescription(description);b.setCoverUrl(cover);return b;}
}
