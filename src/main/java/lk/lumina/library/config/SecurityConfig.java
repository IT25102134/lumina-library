package lk.lumina.library.config;

import lk.lumina.library.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(UserAccountRepository users){
        return username -> users.findByEmailIgnoreCase(username)
            .map(u -> User.withUsername(u.getEmail()).password(u.getPassword())
                .roles(u.getRole().name()).disabled(!u.isActive()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .requestMatchers("/catalog/manage/**").hasRole("HEAD_LIBRARIAN")
                .requestMatchers("/inventory/**").hasRole("LIBRARY_ASSISTANT")
                .requestMatchers("/circulation/**").hasRole("CIRCULATION_STAFF")
                .requestMatchers("/events/manage/**").hasRole("EVENT_COORDINATOR")
                .requestMatchers("/feedback/manage/**").hasRole("LIBRARY_MANAGER")
                .requestMatchers("/audit/**").hasRole("LIBRARY_MANAGER")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/dashboard",true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/?logout").permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));
        return http.build();
    }
}
