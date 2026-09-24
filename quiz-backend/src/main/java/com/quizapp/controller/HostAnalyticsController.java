package com.quizapp.controller;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizAttempt;
import com.quizapp.repository.QuizAttemptRepository;
import com.quizapp.repository.QuizJoinRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.security.HostAuthorization;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/quizzes")
@PreAuthorize("@hostAuthorization.approved(authentication)")
public class HostAnalyticsController {
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final QuizJoinRepository joins;

    public HostAnalyticsController(QuizRepository quizzes, QuizAttemptRepository attempts, QuizJoinRepository joins) {
        this.quizzes = quizzes; this.attempts = attempts; this.joins = joins;
    }

    @GetMapping("/{quizId}/analytics")
    public ResponseEntity<?> analytics(@PathVariable String quizId, Authentication auth) {
        Quiz quiz = quizzes.findById(quizId).orElse(null);
        if (quiz == null || !auth.getName().equals(quiz.getHostId())) return ResponseEntity.notFound().build();
        List<QuizAttempt> submitted = attempts.findByQuizIdAndStatusOrderByScoreDescSubmittedAtAsc(quizId, "SUBMITTED");
        int totalQuestions = quiz.getQuestions().size();
        int joined = joins.findByQuizId(quizId).size();
        double averageScore = submitted.stream().mapToInt(QuizAttempt::getScore).average().orElse(0);
        int maxScore = totalQuestions * Math.max(1, quiz.getPointsPerCorrect());
        double averagePct = maxScore == 0 ? 0 : Math.max(0, averageScore * 100.0 / maxScore);
        double completionRate = joined == 0 ? 0 : submitted.size() * 100.0 / joined;
        double averageTime = submitted.stream().filter(a -> a.getStartedAt()!=null && a.getSubmittedAt()!=null)
                .mapToLong(a -> Math.max(0, java.time.Duration.between(a.getStartedAt(), a.getSubmittedAt()).getSeconds())).average().orElse(0);

        List<Map<String,Object>> questions = new ArrayList<>();
        for (int i=0; i<totalQuestions; i++) {
            Quiz.Question q = quiz.getQuestions().get(i);
            int answered = 0, correct = 0;
            for (QuizAttempt a : submitted) {
                String given = a.getAnswers() == null ? null : a.getAnswers().get(i);
                if (given != null && !given.trim().isEmpty()) {
                    answered++;
                    if (q.getCorrectAnswer()!=null && q.getCorrectAnswer().trim().equalsIgnoreCase(given.trim())) correct++;
                }
            }
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("index", i); row.put("question", q.getQuestion()); row.put("type", q.getType());
            row.put("answered", answered); row.put("correct", correct);
            row.put("accuracy", answered == 0 ? 0 : Math.round(correct * 10000.0 / answered) / 100.0);
            row.put("difficulty", answered == 0 ? "NO DATA" : correct * 100.0 / answered < 40 ? "HARD" : correct * 100.0 / answered < 70 ? "MEDIUM" : "EASY");
            questions.add(row);
        }

        Map<String,Integer> distribution = new LinkedHashMap<>();
        distribution.put("0-24%",0); distribution.put("25-49%",0); distribution.put("50-74%",0); distribution.put("75-99%",0); distribution.put("100%",0);
        for (QuizAttempt a : submitted) {
            double pct = maxScore == 0 ? 0 : Math.max(0, a.getScore() * 100.0 / maxScore);
            if (pct >= 100) distribution.computeIfPresent("100%", (k,v)->v+1);
            else if (pct >= 75) distribution.computeIfPresent("75-99%", (k,v)->v+1);
            else if (pct >= 50) distribution.computeIfPresent("50-74%", (k,v)->v+1);
            else if (pct >= 25) distribution.computeIfPresent("25-49%", (k,v)->v+1);
            else distribution.computeIfPresent("0-24%", (k,v)->v+1);
        }
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("quizId", quizId);
        response.put("title", quiz.getTitle());
        response.put("totalQuestions", totalQuestions);
        response.put("pointsPerCorrect", quiz.getPointsPerCorrect());
        response.put("negativeMarkingEnabled", quiz.isNegativeMarkingEnabled());
        response.put("penaltyPerWrong", quiz.getPenaltyPerWrong());
        response.put("maxScore", maxScore);
        response.put("joinedParticipants", joined);
        response.put("submittedAttempts", submitted.size());
        response.put("completionRate", Math.round(completionRate * 10.0) / 10.0);
        response.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
        response.put("averagePercentage", Math.round(averagePct * 10.0) / 10.0);
        response.put("averageTimeSeconds", Math.round(averageTime));
        response.put("scoreDistribution", distribution);
        response.put("questions", questions);
        return ResponseEntity.ok(response);
    }
}
