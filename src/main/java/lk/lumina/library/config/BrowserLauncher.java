package lk.lumina.library.config;

import java.awt.Desktop;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BrowserLauncher {
    @Value("${app.open-browser:true}") private boolean openBrowser;
    @EventListener(ApplicationReadyEvent.class)
    public void launch(ApplicationReadyEvent event) {
        if (!openBrowser) return;
        int port = event.getApplicationContext() instanceof WebServerApplicationContext context
            ? context.getWebServer().getPort() : 8081;
        String url = "http://localhost:" + port;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) { }

        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            else if (os.contains("mac")) new ProcessBuilder("open", url).start();
            else new ProcessBuilder("xdg-open", url).start();
        } catch (Exception ignored) {
            System.out.println("Open Lumina in your browser: " + url);
        }
    }
}
