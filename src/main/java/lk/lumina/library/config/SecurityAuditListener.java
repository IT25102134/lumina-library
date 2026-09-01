package lk.lumina.library.config;

import lk.lumina.library.service.AuditService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditListener {
    private final AuditService audit;
    public SecurityAuditListener(AuditService audit){this.audit=audit;}
    @EventListener public void login(AuthenticationSuccessEvent event){audit.record(event.getAuthentication().getName(),"LOGIN","Session",null,"Successful authentication");}
    @EventListener public void logout(LogoutSuccessEvent event){if(event.getAuthentication()!=null)audit.record(event.getAuthentication().getName(),"LOGOUT","Session",null,"Session ended");}
}
