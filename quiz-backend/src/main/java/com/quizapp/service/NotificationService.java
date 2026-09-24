package com.quizapp.service;

import com.quizapp.model.Notification;
import com.quizapp.repository.NotificationRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    public NotificationService(NotificationRepository repository){this.repository=repository;}

    public Notification send(String userId, String title, String message, String type, String link){
        if(userId==null || userId.isBlank()) return null;
        Notification n=new Notification(); n.setUserId(userId); n.setTitle(title); n.setMessage(message);
        n.setType(type==null?"INFO":type); n.setLink(link); n.setCreatedAt(Instant.now()); n.setRead(false);
        return repository.save(n);
    }
}
