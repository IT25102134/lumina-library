package lk.lumina.library.circulation.validation;

import lk.lumina.library.repository.UserAccountRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the Circulation Desk-only profile validator for the shared profile URL. */
@Configuration
class CirculationProfileWebConfig implements WebMvcConfigurer {
    private final UserAccountRepository users;

    CirculationProfileWebConfig(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CirculationProfileValidationInterceptor(users))
                .addPathPatterns("/profile", "/dashboard");
    }
}
