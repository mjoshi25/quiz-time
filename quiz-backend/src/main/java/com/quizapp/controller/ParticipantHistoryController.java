package com.quizapp.controller;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizAttempt;
import com.quizapp.repository.QuizAttemptRepository;
import com.quizapp.repository.QuizRepository;
import java.time.Duration;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participant/history")
@PreAuthorize("hasRole('PARTICIPANT')")
public class ParticipantHistoryController {
    private final QuizAttemptRepository attempts;
    private final QuizRepository quizzes;
    public ParticipantHistoryController(QuizAttemptRepository attempts, QuizRepository quizzes){this.attempts=attempts;this.quizzes=quizzes;}

    @GetMapping
    public ResponseEntity<?> history(Authentication auth){
        List<QuizAttempt> all = attempts.findByParticipantIdOrderBySubmittedAtDesc(auth.getName());
        List<Map<String,Object>> rows = new ArrayList<>();
        for(QuizAttempt a: all){
            Quiz q=quizzes.findById(a.getQuizId()).orElse(null);
            int total=a.getTotalQuestions();
            Map<String,Object> r=new LinkedHashMap<>();
            r.put("attemptId",a.getId()); r.put("quizId",a.getQuizId()); r.put("title",q==null?"Quiz":q.getTitle());
            r.put("status",a.getStatus()); r.put("score",a.getScore()); r.put("correctAnswers",a.getCorrectAnswers()); r.put("totalQuestions",total);
            int maxScore = total * Math.max(1, q == null ? 1 : q.getPointsPerCorrect());
            r.put("maxScore", maxScore);
            r.put("percentage",maxScore==0?0:Math.max(0,Math.round(a.getScore()*10000.0/maxScore)/100.0));
            r.put("startedAt",a.getStartedAt()); r.put("submittedAt",a.getSubmittedAt());
            r.put("timeTakenSeconds",a.getStartedAt()!=null&&a.getSubmittedAt()!=null?Math.max(0,Duration.between(a.getStartedAt(),a.getSubmittedAt()).getSeconds()):null);
            rows.add(r);
        }
        return ResponseEntity.ok(Map.of("attempts",rows));
    }
}
