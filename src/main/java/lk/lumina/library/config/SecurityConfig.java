package lk.lumina.library.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lk.lumina.library.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(UserAccountRepository users){
        return username -> users.findByEmailIgnoreCase(username)
                .map(u -> User.withUsername(u.getEmail()).password(u.getPassword())
                        .roles(u.getRole().name()).disabled(!u.isActive()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean AuthenticationSuccessHandler loginSuccessHandler(){
        return (HttpServletRequest req, HttpServletResponse res, Authentication auth) -> {
            boolean isCoordinator = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_EVENT_COORDINATOR"));
            res.sendRedirect(req.getContextPath() + (isCoordinator ? "/events" : "/dashboard"));
        };
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/catalog/manage/**").hasRole("HEAD_LIBRARIAN")
                        .requestMatchers("/inventory/**").hasRole("LIBRARY_ASSISTANT")
                        .requestMatchers("/circulation/**").hasAnyRole("CIRCULATION_STAFF","HEAD_LIBRARIAN")
                        .requestMatchers("/events/manage/**").hasAnyRole("EVENT_COORDINATOR","HEAD_LIBRARIAN")
                        .requestMatchers("/feedback/manage/**").hasAnyRole("LIBRARY_MANAGER","HEAD_LIBRARIAN")
                        .requestMatchers("/audit/**").hasAnyRole("LIBRARY_MANAGER","HEAD_LIBRARIAN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").successHandler(loginSuccessHandler()).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/?logout").permitAll())
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));
        return http.build();
    }
}