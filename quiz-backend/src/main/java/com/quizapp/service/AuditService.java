package com.quizapp.service;

import com.quizapp.model.AuditLog;
import com.quizapp.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repo;
    public AuditService(AuditLogRepository repo){this.repo=repo;}
    public void log(Authentication auth,String action,String type,String id,String name,String details){
        if(auth==null) return;
        AuditLog x=new AuditLog(); x.setActorId(auth.getName()); x.setActorEmail(auth.getName());
        x.setActorRole(auth.getAuthorities().stream().findFirst().map(a->a.getAuthority().replace("ROLE_","")).orElse("UNKNOWN"));
        x.setAction(action); x.setEntityType(type); x.setEntityId(id); x.setEntityName(name); x.setDetails(details); repo.save(x);
    }
}
