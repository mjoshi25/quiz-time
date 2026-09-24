package com.quizapp.controller;

import com.quizapp.model.AuditLog;
import com.quizapp.repository.AuditLogRepository;
import java.time.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {
    private final AuditLogRepository repo;
    public AdminAuditController(AuditLogRepository repo){this.repo=repo;}

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required=false) String q,
                                  @RequestParam(required=false) String action,
                                  @RequestParam(required=false) String from,
                                  @RequestParam(required=false) String to){
        List<AuditLog> rows;
        if(q!=null && !q.isBlank()) rows=repo.findByActorEmailContainingIgnoreCaseOrActionContainingIgnoreCaseOrEntityNameContainingIgnoreCaseOrderByCreatedAtDesc(q,q,q);
        else if(from!=null && !from.isBlank() || to!=null && !to.isBlank()) {
            Instant f=parseDate(from, false); Instant t=parseDate(to, true); rows=repo.findByCreatedAtBetweenOrderByCreatedAtDesc(f,t);
        } else rows=repo.findTop500ByOrderByCreatedAtDesc();
        if(action!=null && !action.isBlank()) rows=rows.stream().filter(x->action.equalsIgnoreCase(x.getAction())).toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping(value="/export",produces="text/csv")
    public ResponseEntity<String> export(@RequestParam(required=false) String q,@RequestParam(required=false) String action,@RequestParam(required=false) String from,@RequestParam(required=false) String to){
        @SuppressWarnings("unchecked") List<AuditLog> rows=(List<AuditLog>)list(q,action,from,to).getBody();
        StringBuilder s=new StringBuilder("Timestamp,Actor,Role,Action,Entity Type,Entity ID,Entity Name,Details\n");
        for(AuditLog x:rows)s.append(cell(String.valueOf(x.getCreatedAt()))).append(',').append(cell(x.getActorEmail())).append(',').append(cell(x.getActorRole())).append(',').append(cell(x.getAction())).append(',').append(cell(x.getEntityType())).append(',').append(cell(x.getEntityId())).append(',').append(cell(x.getEntityName())).append(',').append(cell(x.getDetails())).append('\n');
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename=quizora-audit-log.csv").body(s.toString());
    }
    private Instant parseDate(String v,boolean end){try{return (v==null||v.isBlank())?(end?Instant.now():Instant.EPOCH):(end?LocalDate.parse(v).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant():LocalDate.parse(v).atStartOfDay(ZoneOffset.UTC).toInstant());}catch(Exception e){return end?Instant.now():Instant.EPOCH;}}
    private String cell(String v){return "\""+(v==null?"":v.replace("\"","\"\""))+"\"";}
}
