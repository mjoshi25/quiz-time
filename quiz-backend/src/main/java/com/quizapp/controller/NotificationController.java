package com.quizapp.controller;

import com.quizapp.repository.NotificationRepository;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository notifications;
    public NotificationController(NotificationRepository notifications){this.notifications=notifications;}

    @GetMapping
    public Object list(Authentication auth){return notifications.findTop50ByUserIdOrderByCreatedAtDesc(auth.getName());}

    @GetMapping("/unread-count")
    public Object unread(Authentication auth){return Map.of("count", notifications.countByUserIdAndReadFalse(auth.getName()));}

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> read(@PathVariable String id, Authentication auth){
        var n=notifications.findById(id).orElse(null);
        if(n==null) return ResponseEntity.notFound().build();
        if(!auth.getName().equals(n.getUserId())) return ResponseEntity.status(403).body("You do not own this notification");
        n.setRead(true); return ResponseEntity.ok(notifications.save(n));
    }

    @PatchMapping("/read-all")
    public Object readAll(Authentication auth){
        var list=notifications.findTop50ByUserIdOrderByCreatedAtDesc(auth.getName());
        list.forEach(n->n.setRead(true)); notifications.saveAll(list);
        return Map.of("updated",list.size());
    }
}
