package com.quizapp.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.quizapp.repository.UserRepository;

@RestController
@RequestMapping("/api/host")
@PreAuthorize("@hostAuthorization.approved(authentication)")
public class HostController {
    private final UserRepository users;
    public HostController(UserRepository users){this.users=users;}
    @GetMapping("/dashboard")
    Object dashboard(Authentication auth){
        var u=users.findById(auth.getName()).orElse(null);
        return java.util.Map.of("message","Approved host access granted", "quizPostingEnabled", u==null || u.isQuizPostingEnabled());
    }
}
