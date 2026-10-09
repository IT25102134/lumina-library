package lk.lumina.library.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Encrypt and verify user passwords.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Load user information from the database.
    @Bean
    UserDetailsService userDetailsService(UserAccountRepository users) {

        return username -> users.findByEmailIgnoreCase(username)
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getPassword())
                        .roles(u.getRole().name())
                        .disabled(!u.isActive())
                        .build())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Account not found"));
    }

    // Preserve the main branch's role-based login redirect.
    @Bean
    AuthenticationSuccessHandler loginSuccessHandler() {

        return (HttpServletRequest req,
                HttpServletResponse res,
                Authentication auth) -> {

            boolean isCoordinator = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority()
                            .equals("ROLE_EVENT_COORDINATOR"));

            res.sendRedirect(req.getContextPath()
                    + (isCoordinator ? "/events" : "/dashboard"));
        };
    }

    // Configure URL authorization and login/logout behavior.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth

                        // Public resources.
                        .requestMatchers(
                                "/",
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error"
                        ).permitAll()

                        // Catalog management.
                        .requestMatchers("/catalog/manage/**")
                        .hasRole("HEAD_LIBRARIAN")

                        // Inventory management.
                        .requestMatchers("/inventory/**")
                        .hasRole("LIBRARY_ASSISTANT")

                        // Circulation management.
                        .requestMatchers("/circulation/**")
                        .hasAnyRole("CIRCULATION_STAFF", "HEAD_LIBRARIAN")

                        // Event management.
                        .requestMatchers("/events/manage/**")
                        .hasAnyRole("EVENT_COORDINATOR", "HEAD_LIBRARIAN")

                        // Audit management.
                        .requestMatchers("/audit/**")
                        .hasAnyRole("LIBRARY_MANAGER", "HEAD_LIBRARIAN")

                        // Feedback management:
                        // Allow Library Manager and Head Librarian.
                        // Specific manager routes must come first.
                        .requestMatchers(
                                "/feedback/manage",
                                "/feedback/manage/**"
                        )
                        .hasAnyRole("LIBRARY_MANAGER", "HEAD_LIBRARIAN")

                        // Feedback member operations.
                        .requestMatchers(
                                "/feedback",
                                "/feedback/**"
                        )
                        .hasRole("MEMBER")

                        // All remaining URLs require authentication.
                        .anyRequest().authenticated()
                )

                // Preserve role-based login redirect.
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler())
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout")
                        .permitAll()
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }
}