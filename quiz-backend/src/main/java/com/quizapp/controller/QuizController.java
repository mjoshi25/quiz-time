package com.quizapp.controller;

import com.quizapp.dto.QuizDtos.QuizRequest;
import com.quizapp.model.Quiz;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.QuizJoinRepository;
import java.time.Instant;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.quizapp.service.CompetitionLifecycleService;

@RestController
@RequestMapping("/api/host/quizzes")
@PreAuthorize("@hostAuthorization.approved(authentication)")
public class QuizController {
    private final QuizRepository quizzes;
    private final QuizJoinRepository joins;
    private final CompetitionLifecycleService lifecycle;
    private final com.quizapp.repository.UserRepository users;
    private final com.quizapp.service.NotificationService notifications;
    private final com.quizapp.service.AuditService audit;

    public QuizController(QuizRepository quizzes, QuizJoinRepository joins, CompetitionLifecycleService lifecycle, com.quizapp.repository.UserRepository users, com.quizapp.service.NotificationService notifications, com.quizapp.service.AuditService audit) {
        this.quizzes = quizzes; this.joins = joins; this.lifecycle = lifecycle; this.users = users; this.notifications = notifications; this.audit = audit;
    }

    @GetMapping
    public Object mine(Authentication auth) {
        return quizzes.findByHostIdOrderByCreatedAtDesc(auth.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id, Authentication auth) {
        Quiz quiz = quizzes.findById(id).orElse(null);
        quiz = lifecycle.sync(quiz);
        if (quiz == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(quiz.getHostId())) {
            return ResponseEntity.status(403).body("You do not own this quiz");
        }
        return ResponseEntity.ok(quiz);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody QuizRequest request, Authentication auth) {
        var host = users.findById(auth.getName()).orElse(null);
        if (host == null) return ResponseEntity.status(401).body("Host account not found");
        if (!host.isQuizPostingEnabled()) return ResponseEntity.status(403).body("Administrator has disabled new quiz posting for your host account.");
        return saveQuiz(null, request, auth);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @RequestBody QuizRequest request,
                                    Authentication auth) {
        Quiz existing = quizzes.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(existing.getHostId())) {
            return ResponseEntity.status(403).body("You do not own this quiz");
        }
        return saveQuiz(existing, request, auth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication auth) {
        Quiz existing = quizzes.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(existing.getHostId())) {
            return ResponseEntity.status(403).body("You do not own this quiz");
        }
        joins.deleteByQuizId(id);
        quizzes.delete(existing); audit.log(auth,"QUIZ_DELETED","QUIZ",existing.getId(),existing.getTitle(),"Host deleted competition");
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable String id, Authentication auth) {
        Quiz q = quizzes.findById(id).orElse(null);
        if (q == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(q.getHostId())) {
            return ResponseEntity.status(403).body("You do not own this quiz");
        }
        if (q.getQuestions() == null || q.getQuestions().isEmpty()) return ResponseEntity.badRequest().body("Add at least one question before publishing");
        q.setStatus("PUBLISHED");
        q.setUpdatedAt(Instant.now());
        Quiz saved=quizzes.save(q); audit.log(auth,"QUIZ_PUBLISHED","QUIZ",q.getId(),q.getTitle(),"Host published competition");
        notifications.send(q.getHostId(), "Quiz published", "Your quiz \""+q.getTitle()+"\" has been published successfully.", "QUIZ_PUBLISHED", "/host/quizzes/"+q.getId());
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<?> start(@PathVariable String id, Authentication auth) {
        Quiz q = quizzes.findById(id).orElse(null);
        if (q == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(q.getHostId())) return ResponseEntity.status(403).body("You do not own this quiz");
        if (!"PUBLISHED".equals(q.getStatus())) return ResponseEntity.badRequest().body("Only a published quiz can be started.");
        if (q.getQuestions() == null || q.getQuestions().isEmpty()) return ResponseEntity.badRequest().body("Add at least one question before starting");
        Instant now = Instant.now();
        if (q.getScheduledStartAt() != null && now.isBefore(q.getScheduledStartAt())) {
            return ResponseEntity.status(409).body("The scheduled start time has not been reached yet.");
        }
        if (q.getScheduledEndAt() != null && !now.isBefore(q.getScheduledEndAt())) {
            return ResponseEntity.status(409).body("The scheduled end time has already passed.");
        }
        q.setStatus("STARTED");
        q.setUpdatedAt(now);
        Quiz saved=quizzes.save(q); audit.log(auth,"QUIZ_STARTED","QUIZ",q.getId(),q.getTitle(),"Host started competition");
        notifications.send(q.getHostId(), "Competition started", "Your competition \""+q.getTitle()+"\" is now live.", "QUIZ_STARTED", "/host/quizzes/"+q.getId()+"/monitor");
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/publication")
    public ResponseEntity<?> publication(@PathVariable String id, @RequestBody PublicationRequest request, Authentication auth) {
        Quiz q = quizzes.findById(id).orElse(null);
        if (q == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(q.getHostId())) return ResponseEntity.status(403).body("You do not own this quiz");
        if (request == null) return ResponseEntity.badRequest().body("Publication settings are required");
        if (request.resultPublished() != null) q.setResultPublished(request.resultPublished());
        if (request.certificateEnabled() != null) q.setCertificateEnabled(request.certificateEnabled());
        if (request.certificateTopRanks() != null) {
            if (request.certificateTopRanks() < 1 || request.certificateTopRanks() > 100) return ResponseEntity.badRequest().body("Certificate top ranks must be between 1 and 100");
            q.setCertificateTopRanks(request.certificateTopRanks());
        }
        if (request.prizeDescription() != null) q.setPrizeDescription(request.prizeDescription().trim());
        q.setUpdatedAt(Instant.now());
        Quiz saved=quizzes.save(q);
        audit.log(auth,"QUIZ_PUBLICATION_SETTINGS_UPDATED","QUIZ",q.getId(),q.getTitle(),"Updated result, certificate and prize publication settings");
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable String id, Authentication auth) {
        Quiz q = quizzes.findById(id).orElse(null);
        if (q == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(q.getHostId())) return ResponseEntity.status(403).body("You do not own this quiz");
        if (!"PUBLISHED".equals(q.getStatus()) && !"STARTED".equals(q.getStatus())) return ResponseEntity.badRequest().body("Only a published or started quiz can be closed.");
        q.setStatus("CLOSED");
        q.setUpdatedAt(Instant.now());
        Quiz saved=quizzes.save(q); audit.log(auth,"QUIZ_CLOSED","QUIZ",q.getId(),q.getTitle(),"Host closed competition");
        notifications.send(q.getHostId(), "Competition closed", "Your competition \""+q.getTitle()+"\" has been closed.", "QUIZ_CLOSED", "/host/quizzes/"+q.getId()+"/results");
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<?> reopen(@PathVariable String id, Authentication auth) {
        Quiz q = quizzes.findById(id).orElse(null);
        if (q == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(q.getHostId())) return ResponseEntity.status(403).body("You do not own this quiz");
        if (!"CLOSED".equals(q.getStatus())) return ResponseEntity.badRequest().body("Only a closed quiz can be reopened.");
        q.setStatus("PUBLISHED");
        q.setUpdatedAt(Instant.now());
        Quiz saved=quizzes.save(q); audit.log(auth,"QUIZ_REOPENED","QUIZ",q.getId(),q.getTitle(),"Host reopened competition"); return ResponseEntity.ok(saved);
    }

    private ResponseEntity<?> saveQuiz(Quiz existing, QuizRequest request, Authentication auth) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            return ResponseEntity.badRequest().body("Quiz title is required");
        }
        if (request.questions() == null || request.questions().isEmpty()) {
            return ResponseEntity.badRequest().body("At least one question is required");
        }

        Quiz q = existing == null ? new Quiz() : existing;
        q.setTitle(request.title().trim());
        q.setDescription(request.description() == null ? "" : request.description().trim());
        q.setCategory(request.category() == null || request.category().isBlank() ? "Other" : request.category().trim());
        q.setTopic(request.topic() == null ? "" : request.topic().trim());
        int duration = request.durationMinutes() == null ? (existing == null ? 10 : existing.getDurationMinutes()) : request.durationMinutes();
        if (duration < 1 || duration > 180) return ResponseEntity.badRequest().body("Quiz duration must be between 1 and 180 minutes");
        if (request.scheduledStartAt() != null && request.scheduledEndAt() != null && !request.scheduledEndAt().isAfter(request.scheduledStartAt())) {
            return ResponseEntity.badRequest().body("Scheduled end time must be after scheduled start time");
        }
        q.setDurationMinutes(duration);
        int pointsPerCorrect = request.pointsPerCorrect() == null ? (existing == null ? 1 : existing.getPointsPerCorrect()) : request.pointsPerCorrect();
        if (pointsPerCorrect < 1 || pointsPerCorrect > 100) return ResponseEntity.badRequest().body("Points per correct answer must be between 1 and 100");
        q.setPointsPerCorrect(pointsPerCorrect);
        boolean negativeMarking = request.negativeMarkingEnabled() != null ? request.negativeMarkingEnabled() : (existing != null && existing.isNegativeMarkingEnabled());
        int penaltyPerWrong = request.penaltyPerWrong() == null ? (existing == null ? 0 : existing.getPenaltyPerWrong()) : request.penaltyPerWrong();
        if (penaltyPerWrong < 0 || penaltyPerWrong > 100) return ResponseEntity.badRequest().body("Wrong-answer penalty must be between 0 and 100");
        q.setNegativeMarkingEnabled(negativeMarking);
        q.setPenaltyPerWrong(negativeMarking ? penaltyPerWrong : 0);
        boolean allowRetake = request.allowRetake() != null ? request.allowRetake() : (existing != null && existing.isAllowRetake());
        int maxAttempts = request.maxAttempts() == null ? (existing == null ? 1 : existing.getMaxAttempts()) : request.maxAttempts();
        if (maxAttempts < 1 || maxAttempts > 20) return ResponseEntity.badRequest().body("Maximum attempts must be between 1 and 20");
        q.setAllowRetake(allowRetake);
        q.setMaxAttempts(allowRetake ? maxAttempts : 1);
        if (request.resultPublished() != null) q.setResultPublished(request.resultPublished());
        if (request.certificateEnabled() != null) q.setCertificateEnabled(request.certificateEnabled());
        int certificateTopRanks = request.certificateTopRanks() == null ? (existing == null ? 3 : existing.getCertificateTopRanks()) : request.certificateTopRanks();
        if (certificateTopRanks < 1 || certificateTopRanks > 100) return ResponseEntity.badRequest().body("Certificate top ranks must be between 1 and 100");
        q.setCertificateTopRanks(certificateTopRanks);
        if (request.prizeDescription() != null) q.setPrizeDescription(request.prizeDescription().trim());
        q.setScheduledStartAt(request.scheduledStartAt());
        q.setScheduledEndAt(request.scheduledEndAt());
        if (existing == null) {
            q.setHostId(auth.getName());
            q.setStatus("DRAFT");
        }

        var list = new ArrayList<Quiz.Question>();
        for (var x : request.questions()) {
            if (x == null || x.question() == null || x.question().isBlank()
                    || x.correctAnswer() == null || x.correctAnswer().isBlank()) {
                return ResponseEntity.badRequest().body("Each question requires text and a correct answer");
            }
            Quiz.Question item = new Quiz.Question();
            String type = x.type() == null ? "MCQ" : x.type().trim().toUpperCase();
            item.setType(type);
            item.setQuestion(x.question().trim());
            if ("TRUE_FALSE".equals(type)) {
                item.setOptions(new ArrayList<>(java.util.List.of("True", "False")));
                String answer = x.correctAnswer().trim();
                if (!"true".equalsIgnoreCase(answer) && !"false".equalsIgnoreCase(answer)) {
                    return ResponseEntity.badRequest().body("True / False questions must have True or False as the correct answer");
                }
                item.setCorrectAnswer("true".equalsIgnoreCase(answer) ? "True" : "False");
            } else {
                item.setOptions(x.options() == null ? new ArrayList<>() : x.options());
                item.setCorrectAnswer(x.correctAnswer().trim());
            }
            String difficulty = x.difficulty() == null ? "MEDIUM" : x.difficulty().trim().toUpperCase();
            if (!java.util.Set.of("EASY", "MEDIUM", "HARD").contains(difficulty)) {
                return ResponseEntity.badRequest().body("Question difficulty must be Easy, Medium or Hard");
            }
            item.setDifficulty(difficulty);
            item.setExplanation(x.explanation() == null ? "" : x.explanation().trim());
            item.setImageUrl(x.imageUrl() == null ? "" : x.imageUrl().trim());
            list.add(item);
        }
        q.setQuestions(list);
        q.setUpdatedAt(Instant.now());
        Quiz saved=quizzes.save(q); audit.log(auth, existing==null?"QUIZ_CREATED":"QUIZ_UPDATED", "QUIZ", saved.getId(), saved.getTitle(), existing==null?"Host created competition":"Host updated competition");
        return ResponseEntity.ok(saved);
    }
    public record PublicationRequest(Boolean resultPublished, Boolean certificateEnabled, Integer certificateTopRanks, String prizeDescription) {}
}
