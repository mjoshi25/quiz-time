package com.quizapp.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.*;
import com.quizapp.repository.UserRepository;
import com.quizapp.model.User;
import com.quizapp.dto.AuthDtos.*;
import com.quizapp.security.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    final UserRepository r; final PasswordEncoder p; final JwtService j;
    AuthController(UserRepository r,PasswordEncoder p,JwtService j){this.r=r;this.p=p;this.j=j;}

    private AuthResponse out(User u){
        return new AuthResponse(j.create(u.getId()),u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getHostApprovalStatus(),u.getAccountStatus(),u.getProfilePhoto());
    }

    @PostMapping("/register/participant")
    ResponseEntity<?> participant(@RequestBody RegisterRequest q){return register(q,"PARTICIPANT","NOT_APPLICABLE");}

    @PostMapping("/register/host")
    ResponseEntity<?> host(@RequestBody RegisterRequest q){return register(q,"HOST","PENDING");}

    private ResponseEntity<?> register(RegisterRequest q,String role,String approval){
        if(q.name()==null||q.email()==null||q.password()==null||q.password().length()<8)
            return ResponseEntity.badRequest().body("Name, email and password of at least 8 characters are required");
        if(r.findByEmailIgnoreCase(q.email()).isPresent())return ResponseEntity.status(409).body("Email already registered");
        User u=new User();
        u.setName(q.name().trim());
        u.setEmail(q.email().toLowerCase().trim());
        u.setPasswordHash(p.encode(q.password()));
        u.setRole(role);
        u.setAccountStatus("ACTIVE");
        u.setHostApprovalStatus(approval);
        return ResponseEntity.ok(out(r.save(u)));
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@RequestBody LoginRequest q){
        var u=r.findByEmailIgnoreCase(q.email()).orElse(null);
        if(u==null||!p.matches(q.password(),u.getPasswordHash()))return ResponseEntity.status(401).body("Invalid credentials");
        if(!"ACTIVE".equals(u.getAccountStatus()))return ResponseEntity.status(403).body("Account is not active");
        return ResponseEntity.ok(out(u));
    }

    @GetMapping("/me")
    ResponseEntity<?> me(Authentication auth){
        if(auth==null||!auth.isAuthenticated())return ResponseEntity.status(401).body("Authentication required");
        return r.findById(auth.getName()).<ResponseEntity<?>>map(u->ResponseEntity.ok(new MeResponse(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getAccountStatus(),u.getHostApprovalStatus(),u.getRejectionReason(),u.getProfilePhoto())))
            .orElseGet(()->ResponseEntity.status(404).body("User not found"));
    }

    @PatchMapping("/profile")
    ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest q, Authentication auth){
        if(auth==null||!auth.isAuthenticated())return ResponseEntity.status(401).body("Authentication required");
        var u=r.findById(auth.getName()).orElse(null);
        if(u==null)return ResponseEntity.status(404).body("User not found");
        if(q.name()!=null){
            String name=q.name().trim();
            if(name.isBlank())return ResponseEntity.badRequest().body("Name cannot be empty");
            if(name.length()>100)return ResponseEntity.badRequest().body("Name is too long");
            u.setName(name);
        }
        if(q.profilePhoto()!=null){
            if(!q.profilePhoto().isBlank() && !q.profilePhoto().startsWith("data:image/"))
                return ResponseEntity.badRequest().body("Profile photo must be an image");
            if(q.profilePhoto().length()>3_000_000)
                return ResponseEntity.badRequest().body("Profile photo is too large. Please use an image below 2 MB.");
            u.setProfilePhoto(q.profilePhoto().isBlank()?null:q.profilePhoto());
        }
        u=r.save(u);
        return ResponseEntity.ok(new MeResponse(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getAccountStatus(),u.getHostApprovalStatus(),u.getRejectionReason(),u.getProfilePhoto()));
    }

    public record MeResponse(String id,String name,String email,String role,String accountStatus,String hostApprovalStatus,String rejectionReason,String profilePhoto){}
    public record ProfileUpdateRequest(String name,String profilePhoto){}
}
