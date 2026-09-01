package lk.lumina.library.repository;
import java.util.*;
import lk.lumina.library.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserAccountRepository extends JpaRepository<UserAccount,Long>{
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    List<UserAccount> findByRole(Role role);
}
