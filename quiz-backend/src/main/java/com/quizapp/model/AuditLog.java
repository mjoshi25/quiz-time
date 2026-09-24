package com.quizapp.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("audit_logs")
public class AuditLog {
    @Id private String id;
    private String actorId;
    private String actorEmail;
    private String actorRole;
    private String action;
    private String entityType;
    private String entityId;
    private String entityName;
    private String details;
    private Instant createdAt = Instant.now();
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getActorId(){return actorId;} public void setActorId(String v){actorId=v;}
    public String getActorEmail(){return actorEmail;} public void setActorEmail(String v){actorEmail=v;}
    public String getActorRole(){return actorRole;} public void setActorRole(String v){actorRole=v;}
    public String getAction(){return action;} public void setAction(String v){action=v;}
    public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;}
    public String getEntityId(){return entityId;} public void setEntityId(String v){entityId=v;}
    public String getEntityName(){return entityName;} public void setEntityName(String v){entityName=v;}
    public String getDetails(){return details;} public void setDetails(String v){details=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
