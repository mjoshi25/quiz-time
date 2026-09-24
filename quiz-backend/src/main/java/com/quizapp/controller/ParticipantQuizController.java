package com.quizapp.controller;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizJoin;
import com.quizapp.repository.QuizJoinRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.service.CompetitionLifecycleService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participant/quizzes")
@PreAuthorize("hasRole('PARTICIPANT')")
public class ParticipantQuizController {
    private final QuizRepository quizzes;
    private final QuizJoinRepository joins;
    private final CompetitionLifecycleService lifecycle;

    public ParticipantQuizController(QuizRepository quizzes, QuizJoinRepository joins, CompetitionLifecycleService lifecycle) {
        this.quizzes = quizzes;
        this.joins = joins;
        this.lifecycle = lifecycle;
    }

    @GetMapping("/joined")
    public List<JoinedQuiz> joined(Authentication auth) {
        return joins.findByParticipantIdOrderByJoinedAtDesc(auth.getName()).stream()
                .map(j -> quizzes.findById(j.getQuizId())
                        .map(q -> new JoinedQuiz(q.getId(), q.getTitle(), q.getDescription(), q.getQuestions().size(), q.getStatus(), j.getJoinedAt()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> status(@PathVariable String id, Authentication auth) {
        Quiz quiz = quizzes.findById(id).orElse(null);
        if (quiz == null) return ResponseEntity.notFound().build();
        lifecycle.sync(quiz);
        return ResponseEntity.ok(new JoinStatus(joins.findTopByQuizIdAndParticipantIdOrderByJoinedAtDesc(id, auth.getName()).isPresent()));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> join(@PathVariable String id, Authentication auth) {
        Quiz quiz = quizzes.findById(id).orElse(null);
        if (quiz == null) return ResponseEntity.notFound().build();
        quiz = lifecycle.sync(quiz);
        if (!"STARTED".equals(quiz.getStatus())) {
            if ("PUBLISHED".equals(quiz.getStatus())) return ResponseEntity.status(409).body("The host has published this quiz but has not started the competition yet.");
            if ("CLOSED".equals(quiz.getStatus())) return ResponseEntity.status(409).body("This competition is closed.");
            return ResponseEntity.badRequest().body("This quiz is not open for participants yet.");
        }

        var existing = joins.findTopByQuizIdAndParticipantIdOrderByJoinedAtDesc(id, auth.getName());
        if (existing.isPresent()) {
            return ResponseEntity.ok(new JoinResult(true, "You have already joined this quiz.", existing.get()));
        }

        QuizJoin join = new QuizJoin();
        join.setQuizId(id);
        join.setParticipantId(auth.getName());
        return ResponseEntity.ok(new JoinResult(true, "Quiz joined successfully.", joins.save(join)));
    }

    public record JoinStatus(boolean joined) {}
    public record JoinResult(boolean joined, String message, QuizJoin registration) {}
    public record JoinedQuiz(String id, String title, String description, int questionCount, String status, java.time.Instant joinedAt) {}
}
