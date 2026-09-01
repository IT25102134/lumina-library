package lk.lumina.library.service;

import lk.lumina.library.model.AuditLog;
import lk.lumina.library.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository logs;
    public AuditService(AuditLogRepository logs){this.logs=logs;}
    public void record(String actor,String action,String entityType,Long entityId,String details){
        logs.save(new AuditLog(actor,action,entityType,entityId,details));
    }
}
