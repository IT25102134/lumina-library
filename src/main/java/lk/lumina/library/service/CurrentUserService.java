package lk.lumina.library.service;

import lk.lumina.library.model.UserAccount;
import lk.lumina.library.repository.UserAccountRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserAccountRepository users;
    public CurrentUserService(UserAccountRepository users){this.users=users;}
    public UserAccount get(){
        String email=SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }
}
