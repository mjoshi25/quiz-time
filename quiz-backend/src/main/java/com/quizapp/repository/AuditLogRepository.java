package com.quizapp.repository;

import com.quizapp.model.AuditLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLog,String> {
    List<AuditLog> findTop500ByOrderByCreatedAtDesc();
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
    List<AuditLog> findByActorEmailContainingIgnoreCaseOrActionContainingIgnoreCaseOrEntityNameContainingIgnoreCaseOrderByCreatedAtDesc(String email,String action,String name);
}
