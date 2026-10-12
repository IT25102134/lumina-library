package lk.lumina.library.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadConfig implements WebMvcConfigurer {
    // Exposes catalogue cover images saved on disk through browser URLs beginning with /uploads/.
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolves the same local uploads directory used when CatalogController stores a cover image.
        String location=Path.of("uploads").toAbsolutePath().normalize().toUri().toString();
        // Maps every /uploads/** request to that directory; the trailing slash is required for a directory resource location.
        registry.addResourceHandler("/uploads/**").addResourceLocations(location.endsWith("/")?location:location+"/");
    }
}
