package com.quizapp.controller;

import com.quizapp.dto.QuizDtos.QuizRequest;
import com.quizapp.model.Quiz;
import com.quizapp.model.User;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.QuizJoinRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.CompetitionLifecycleService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final QuizRepository quizzes;
    private final QuizJoinRepository joins;
    private final CompetitionLifecycleService lifecycle;
    private final com.quizapp.service.NotificationService notifications;
    private final com.quizapp.service.AuditService audit;

    public AdminController(UserRepository users, QuizRepository quizzes, QuizJoinRepository joins, CompetitionLifecycleService lifecycle, com.quizapp.service.NotificationService notifications, com.quizapp.service.AuditService audit) {
        this.users = users; this.quizzes = quizzes; this.joins = joins; this.lifecycle = lifecycle; this.notifications = notifications; this.audit = audit;
    }

    @GetMapping("/users")
    public List<AdminUserView> allUsers() { return users.findAll().stream().sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).map(this::view).toList(); }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> user(@PathVariable String id) { return users.findById(id).map(u -> ResponseEntity.ok(view(u))).orElseGet(() -> ResponseEntity.notFound().build()); }

    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody Map<String,Object> body) {
        User u = users.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        if ("ADMIN".equals(u.getRole())) return ResponseEntity.status(409).body("Administrator accounts cannot be edited from this screen.");
        if (body != null) {
            if (body.get("name") != null) { String name = String.valueOf(body.get("name")).trim(); if (name.isBlank()) return ResponseEntity.badRequest().body("Name cannot be empty"); u.setName(name); }
            if (body.get("email") != null) {
                String email = String.valueOf(body.get("email")).trim().toLowerCase();
                var existing = users.findByEmailIgnoreCase(email).orElse(null);
                if (existing != null && !existing.getId().equals(id)) return ResponseEntity.status(409).body("Email already registered");
                if (email.isBlank()) return ResponseEntity.badRequest().body("Email cannot be empty");
                u.setEmail(email);
            }
        }
        return ResponseEntity.ok(view(users.save(u)));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> userStatus(@PathVariable String id, @RequestBody Map<String,Object> body, Authentication auth) {
        User u = users.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        if ("ADMIN".equals(u.getRole())) return ResponseEntity.status(409).body("Administrator accounts cannot be suspended.");
        String status = body == null || body.get("status") == null ? null : String.valueOf(body.get("status")).toUpperCase();
        if (!Set.of("ACTIVE","SUSPENDED").contains(status)) return ResponseEntity.badRequest().body("Status must be ACTIVE or SUSPENDED");
        u.setAccountStatus(status);
        User saved=users.save(u); audit.log(auth,"USER_STATUS_CHANGED","USER",saved.getId(),saved.getName(),"Account status: "+status);
        return ResponseEntity.ok(view(saved));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication auth) {
        User u = users.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        if ("ADMIN".equals(u.getRole())) return ResponseEntity.status(409).body("Administrator accounts cannot be deleted.");
        users.delete(u); audit.log(auth,"USER_DELETED","USER",u.getId(),u.getName(),"User deleted");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hosts/pending")
    public List<AdminUserView> pending() { return users.findByRoleAndHostApprovalStatus("HOST","PENDING").stream().map(this::view).toList(); }

    @GetMapping("/hosts")
    public List<AdminUserView> hosts() { return users.findByRoleOrderByCreatedAtDesc("HOST").stream().map(this::view).toList(); }

    @PatchMapping("/hosts/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id, Authentication auth) { return changeHost(id,"APPROVED",null,auth); }

    @PatchMapping("/hosts/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id,@RequestBody(required=false) Map<String,String> b, Authentication auth) { return changeHost(id,"REJECTED",b==null?null:b.get("reason"),auth); }

    @PatchMapping("/hosts/{id}/quiz-posting")
    public ResponseEntity<?> quizPosting(@PathVariable String id,@RequestBody Map<String,Object> body, Authentication auth){
        User u=users.findById(id).orElse(null);
        if(u==null || !"HOST".equals(u.getRole())) return ResponseEntity.notFound().build();
        Object value=body==null?null:body.get("enabled");
        if(!(value instanceof Boolean)) return ResponseEntity.badRequest().body("enabled must be true or false");
        u.setQuizPostingEnabled((Boolean)value);
        User saved=users.save(u); audit.log(auth,saved.isQuizPostingEnabled()?"HOST_POSTING_ENABLED":"HOST_POSTING_DISABLED","HOST",saved.getId(),saved.getName(),"Quiz posting changed");
        notifications.send(saved.getId(), saved.isQuizPostingEnabled()?"Quiz posting enabled":"Quiz posting disabled", saved.isQuizPostingEnabled()?"Your administrator has enabled new quiz posting for your account.":"Your administrator has disabled new quiz posting. Existing competitions remain available.", "HOST_POSTING", "/host");
        return ResponseEntity.ok(view(saved));
    }

    @GetMapping("/quizzes")
    public List<Quiz> allQuizzes() {
        return quizzes.findAll().stream().map(lifecycle::sync)
                .sorted(Comparator.comparing(Quiz::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @GetMapping("/quizzes/{id}")
    public ResponseEntity<?> getQuiz(@PathVariable String id) {
        Quiz q=quizzes.findById(id).orElse(null);
        if(q==null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(lifecycle.sync(q));
    }

    @PutMapping("/quizzes/{id}")
    public ResponseEntity<?> updateQuiz(@PathVariable String id, @RequestBody QuizRequest request) {
        Quiz q=quizzes.findById(id).orElse(null);
        if(q==null) return ResponseEntity.notFound().build();
        return saveQuiz(q, request);
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable String id) {
        Quiz q=quizzes.findById(id).orElse(null);
        if(q==null) return ResponseEntity.notFound().build();
        joins.deleteByQuizId(id);
        quizzes.delete(q);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/quizzes/{id}/suspend")
    public ResponseEntity<?> suspendQuiz(@PathVariable String id) {
        Quiz q=quizzes.findById(id).orElse(null);
        if(q==null) return ResponseEntity.notFound().build();
        if("SUSPENDED".equals(q.getStatus())) return ResponseEntity.ok(q);
        q.setStatusBeforeSuspension(q.getStatus());
        q.setStatus("SUSPENDED");
        q.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(quizzes.save(q));
    }

    @PatchMapping("/quizzes/{id}/resume")
    public ResponseEntity<?> resumeQuiz(@PathVariable String id) {
        Quiz q=quizzes.findById(id).orElse(null);
        if(q==null) return ResponseEntity.notFound().build();
        if(!"SUSPENDED".equals(q.getStatus())) return ResponseEntity.ok(q);
        String previous=q.getStatusBeforeSuspension();
        if(previous==null || "SUSPENDED".equals(previous)) previous="PUBLISHED";
        q.setStatus(previous);
        q.setStatusBeforeSuspension(null);
        q.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(quizzes.save(q));
    }

    private ResponseEntity<?> changeHost(String id,String s,String reason, Authentication auth){
        var u=users.findById(id).orElse(null);
        if(u==null || !"HOST".equals(u.getRole())) return ResponseEntity.notFound().build();
        u.setHostApprovalStatus(s);
        u.setRejectionReason(reason);
        if("APPROVED".equals(s) && u.getAccountStatus()==null) u.setAccountStatus("ACTIVE");
        User saved=users.save(u); audit.log(auth,"HOST_"+s,"HOST",saved.getId(),saved.getName(),reason==null?"Host approval updated":reason);
        if("APPROVED".equals(s)) notifications.send(saved.getId(), "Host account approved", "Your Quizora host account has been approved. You can now create and publish competitions.", "HOST_APPROVED", "/host");
        if("REJECTED".equals(s)) notifications.send(saved.getId(), "Host approval update", "Your host registration was rejected." + (reason==null || reason.isBlank()?"":" Reason: "+reason), "HOST_REJECTED", "/host");
        return ResponseEntity.ok(view(saved));
    }

    private AdminUserView view(User u) {
        return new AdminUserView(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getAccountStatus(),
                u.getHostApprovalStatus(), u.getRejectionReason(), u.isQuizPostingEnabled(), u.getCreatedAt());
    }

    public record AdminUserView(String id, String name, String email, String role, String accountStatus,
                                String hostApprovalStatus, String rejectionReason, boolean quizPostingEnabled, Instant createdAt) {}

    private ResponseEntity<?> saveQuiz(Quiz q, QuizRequest request) {
        if(request==null || request.title()==null || request.title().isBlank()) return ResponseEntity.badRequest().body("Quiz title is required");
        if(request.questions()==null || request.questions().isEmpty()) return ResponseEntity.badRequest().body("At least one question is required");
        int duration=request.durationMinutes()==null?q.getDurationMinutes():request.durationMinutes();
        if(duration<1 || duration>180) return ResponseEntity.badRequest().body("Quiz duration must be between 1 and 180 minutes");
        if(request.scheduledStartAt()!=null && request.scheduledEndAt()!=null && !request.scheduledEndAt().isAfter(request.scheduledStartAt())) return ResponseEntity.badRequest().body("Scheduled end time must be after scheduled start time");
        q.setTitle(request.title().trim()); q.setDescription(request.description()==null?"":request.description().trim()); q.setDurationMinutes(duration);
        int points=request.pointsPerCorrect()==null?q.getPointsPerCorrect():request.pointsPerCorrect();
        if(points<1||points>100) return ResponseEntity.badRequest().body("Points per correct answer must be between 1 and 100");
        boolean negative=request.negativeMarkingEnabled()!=null?request.negativeMarkingEnabled():q.isNegativeMarkingEnabled();
        int penalty=request.penaltyPerWrong()==null?q.getPenaltyPerWrong():request.penaltyPerWrong();
        if(penalty<0||penalty>100) return ResponseEntity.badRequest().body("Wrong-answer penalty must be between 0 and 100");
        q.setPointsPerCorrect(points); q.setNegativeMarkingEnabled(negative); q.setPenaltyPerWrong(negative?penalty:0);
        boolean allowRetake=request.allowRetake()!=null?request.allowRetake():q.isAllowRetake();
        int maxAttempts=request.maxAttempts()==null?q.getMaxAttempts():request.maxAttempts();
        if(maxAttempts<1||maxAttempts>20) return ResponseEntity.badRequest().body("Maximum attempts must be between 1 and 20");
        q.setAllowRetake(allowRetake); q.setMaxAttempts(allowRetake?maxAttempts:1);
        q.setScheduledStartAt(request.scheduledStartAt()); q.setScheduledEndAt(request.scheduledEndAt());
        var list=new ArrayList<Quiz.Question>();
        for(var x:request.questions()){
            if(x==null || x.question()==null || x.question().isBlank() || x.correctAnswer()==null || x.correctAnswer().isBlank()) return ResponseEntity.badRequest().body("Each question requires text and a correct answer");
            Quiz.Question item=new Quiz.Question(); String type=x.type()==null?"MCQ":x.type().trim().toUpperCase(); item.setType(type); item.setQuestion(x.question().trim());
            if("TRUE_FALSE".equals(type)){ item.setOptions(new ArrayList<>(List.of("True","False"))); String a=x.correctAnswer().trim(); if(!"true".equalsIgnoreCase(a)&&!"false".equalsIgnoreCase(a)) return ResponseEntity.badRequest().body("True / False questions must have True or False as the correct answer"); item.setCorrectAnswer("true".equalsIgnoreCase(a)?"True":"False"); }
            else { item.setOptions(x.options()==null?new ArrayList<>():x.options()); item.setCorrectAnswer(x.correctAnswer().trim()); }
            list.add(item);
        }
        q.setQuestions(list); q.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(quizzes.save(q));
    }
}
