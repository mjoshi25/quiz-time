package com.quizapp.controller;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizAttempt;
import com.quizapp.model.QuizJoin;
import com.quizapp.model.User;
import com.quizapp.repository.QuizAttemptRepository;
import com.quizapp.repository.QuizJoinRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.security.HostAuthorization;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/quizzes")
@PreAuthorize("@hostAuthorization.approved(authentication)")
public class HostResultsController {
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final QuizJoinRepository joins;
    private final UserRepository users;

    public HostResultsController(QuizRepository quizzes, QuizAttemptRepository attempts,
                                 QuizJoinRepository joins, UserRepository users) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.joins = joins;
        this.users = users;
    }

    @GetMapping("/{quizId}/monitor")
    public ResponseEntity<?> monitor(@PathVariable String quizId, Authentication auth) {
        Quiz quiz = ownedQuiz(quizId, auth.getName());
        if (quiz == null) return ResponseEntity.notFound().build();

        List<QuizJoin> allJoins = joins.findByQuizId(quizId);
        Map<String, QuizJoin> uniqueJoins = new LinkedHashMap<>();
        for (QuizJoin join : allJoins) uniqueJoins.putIfAbsent(join.getParticipantId(), join);

        List<QuizAttempt> allAttempts = attempts.findAll().stream()
                .filter(a -> quizId.equals(a.getQuizId()))
                .sorted(Comparator.comparing(QuizAttempt::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        Map<String, QuizAttempt> latest = new LinkedHashMap<>();
        for (QuizAttempt a : allAttempts) latest.putIfAbsent(a.getParticipantId(), a);

        List<Map<String,Object>> rows = new ArrayList<>();
        int submitted = 0, active = 0;
        Map<String,Integer> activityTotals = new TreeMap<>();
        for (QuizJoin join : uniqueJoins.values()) {
            User p = users.findById(join.getParticipantId()).orElse(null);
            QuizAttempt a = latest.get(join.getParticipantId());
            String status = a == null ? "JOINED" : ("SUBMITTED".equals(a.getStatus()) ? "SUBMITTED" : "IN_PROGRESS");
            if ("SUBMITTED".equals(status)) submitted++;
            if ("IN_PROGRESS".equals(status)) active++;
            if (a != null && a.getActivityEvents() != null) {
                a.getActivityEvents().forEach((k,v) -> activityTotals.merge(k, v == null ? 0 : v, Integer::sum));
            }
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("participantId", join.getParticipantId());
            row.put("name", p == null ? "Participant" : p.getName());
            row.put("email", p == null ? "" : p.getEmail());
            row.put("status", status);
            row.put("joinedAt", join.getJoinedAt());
            row.put("startedAt", a == null ? null : a.getStartedAt());
            row.put("submittedAt", a == null ? null : a.getSubmittedAt());
            row.put("answered", a == null || a.getAnswers() == null ? 0 : a.getAnswers().size());
            row.put("totalQuestions", quiz.getQuestions().size());
            row.put("score", a != null && "SUBMITTED".equals(a.getStatus()) ? a.getScore() : null);
            row.put("activityEvents", a == null || a.getActivityEvents() == null ? Map.of() : a.getActivityEvents());
            rows.add(row);
        }

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("quizId", quizId);
        out.put("title", quiz.getTitle());
        out.put("status", quiz.getStatus());
        out.put("durationMinutes", quiz.getDurationMinutes());
        out.put("pointsPerCorrect", quiz.getPointsPerCorrect());
        out.put("negativeMarkingEnabled", quiz.isNegativeMarkingEnabled());
        out.put("penaltyPerWrong", quiz.getPenaltyPerWrong());
        out.put("joinedParticipants", uniqueJoins.size());
        out.put("activeParticipants", active);
        out.put("submittedParticipants", submitted);
        out.put("waitingParticipants", Math.max(0, uniqueJoins.size() - active - submitted));
        out.put("activityTotals", activityTotals);
        out.put("participants", rows);
        out.put("serverTime", Instant.now());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{quizId}/results")
    public ResponseEntity<?> results(@PathVariable String quizId, Authentication auth) {
        Quiz quiz = ownedQuiz(quizId, auth.getName());
        if (quiz == null) return ResponseEntity.notFound().build();

        List<QuizAttempt> submitted = rankedAttempts(quizId);
        List<QuizJoin> allJoins = joins.findByQuizId(quizId);
        Map<String, QuizJoin> joinMap = new LinkedHashMap<>();
        for (QuizJoin join : allJoins) joinMap.putIfAbsent(join.getParticipantId(), join);

        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 0; int previousScore = -1; int previousCorrect = -1; long previousTime = -1;
        for (int i = 0; i < submitted.size(); i++) {
            QuizAttempt attempt = submitted.get(i);
            long time = timeTaken(attempt);
            boolean sameRank = i > 0 && attempt.getScore() == previousScore
                    && attempt.getCorrectAnswers() == previousCorrect && time == previousTime;
            if (!sameRank) rank = i + 1;
            User participant = users.findById(attempt.getParticipantId()).orElse(null);
            rows.add(resultRow(quiz, attempt, participant, rank));
            previousScore = attempt.getScore(); previousCorrect = attempt.getCorrectAnswers(); previousTime = time;
        }

        long pending = joinMap.values().stream().filter(j -> !submitted.stream().anyMatch(a -> a.getParticipantId().equals(j.getParticipantId()))).count();
        return ResponseEntity.ok(Map.of(
                "quizId", quizId,
                "title", quiz.getTitle(),
                "status", quiz.getStatus(),
                "totalQuestions", quiz.getQuestions().size(),
                "joinedParticipants", joinMap.size(),
                "submittedAttempts", submitted.size(),
                "notAttemptedOrIncomplete", pending,
                "results", rows
        ));
    }

    @GetMapping(value = "/{quizId}/results/export", produces = "text/csv")
    public ResponseEntity<String> exportResults(@PathVariable String quizId, Authentication auth) {
        Quiz quiz = ownedQuiz(quizId, auth.getName());
        if (quiz == null) return ResponseEntity.notFound().build();
        List<QuizAttempt> submitted = rankedAttempts(quizId);
        StringBuilder csv = new StringBuilder();
        csv.append("Rank,Participant,Email,Score,Max Score,Percentage,Correct Answers,Time Taken (seconds),Started At,Submitted At\n");
        int rank = 0; int previousScore = -1; int previousCorrect = -1; long previousTime = -1;
        for (int i = 0; i < submitted.size(); i++) {
            QuizAttempt a = submitted.get(i);
            long time = timeTaken(a);
            boolean sameRank = i > 0 && a.getScore() == previousScore && a.getCorrectAnswers() == previousCorrect && time == previousTime;
            if (!sameRank) rank = i + 1;
            User participant = users.findById(a.getParticipantId()).orElse(null);
            Map<String,Object> row = resultRow(quiz, a, participant, rank);
            csv.append(rank).append(',')
                    .append(csvCell(participant == null ? "Participant" : participant.getName())).append(',')
                    .append(csvCell(participant == null ? "" : participant.getEmail())).append(',')
                    .append(row.get("score")).append(',').append(row.get("maxScore")).append(',')
                    .append(row.get("percentage")).append(',').append(row.get("correctAnswers")).append(',')
                    .append(row.get("timeTakenSeconds")).append(',').append(csvCell(String.valueOf(row.get("startedAt")))).append(',')
                    .append(csvCell(String.valueOf(row.get("submittedAt")))).append('\n');
            previousScore = a.getScore(); previousCorrect = a.getCorrectAnswers(); previousTime = time;
        }
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=quizora-results-" + quizId + ".csv").body(csv.toString());
    }

    @GetMapping("/{quizId}/question-analytics")
    public ResponseEntity<?> questionAnalytics(@PathVariable String quizId, Authentication auth) {
        Quiz quiz = ownedQuiz(quizId, auth.getName());
        if (quiz == null) return ResponseEntity.notFound().build();
        List<QuizAttempt> submitted = rankedAttempts(quizId);
        List<Map<String,Object>> rows = new ArrayList<>();
        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            Quiz.Question q = quiz.getQuestions().get(i);
            int answered = 0, correct = 0;
            long totalSeconds = 0;
            for (QuizAttempt a : submitted) {
                String answer = a.getAnswers() == null ? null : a.getAnswers().get(i);
                if (answer != null && !answer.isBlank()) {
                    answered++;
                    if (q.getCorrectAnswer() != null && q.getCorrectAnswer().trim().equalsIgnoreCase(answer.trim())) correct++;
                }
                if (a.getQuestionTimeSeconds() != null) totalSeconds += Math.max(0, a.getQuestionTimeSeconds().getOrDefault(i, 0));
            }
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("index", i); row.put("question", q.getQuestion()); row.put("type", q.getType());
            row.put("difficulty", q.getDifficulty()); row.put("submissions", submitted.size());
            row.put("answered", answered); row.put("correct", correct);
            row.put("incorrect", Math.max(0, answered - correct)); row.put("unanswered", Math.max(0, submitted.size() - answered));
            row.put("accuracy", answered == 0 ? 0 : Math.round(correct * 10000.0 / answered) / 100.0);
            row.put("averageTimeSeconds", submitted.isEmpty() ? 0 : Math.round(totalSeconds * 10.0 / submitted.size()) / 10.0);
            rows.add(row);
        }
        return ResponseEntity.ok(Map.of("quizId", quizId, "title", quiz.getTitle(), "submittedAttempts", submitted.size(), "questions", rows));
    }

    private String csvCell(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    @GetMapping("/{quizId}/participants")
    public ResponseEntity<?> participants(@PathVariable String quizId, Authentication auth) {
        Quiz quiz = ownedQuiz(quizId, auth.getName());
        if (quiz == null) return ResponseEntity.notFound().build();

        List<QuizJoin> allJoins = joins.findByQuizId(quizId);
        Map<String, QuizJoin> uniqueJoins = new LinkedHashMap<>();
        for (QuizJoin join : allJoins) uniqueJoins.putIfAbsent(join.getParticipantId(), join);
        List<QuizAttempt> allAttempts = rankedAttempts(quizId);
        Map<String, QuizAttempt> submittedByParticipant = new HashMap<>();
        for (QuizAttempt a : allAttempts) submittedByParticipant.put(a.getParticipantId(), a);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (QuizJoin join : uniqueJoins.values()) {
            User p = users.findById(join.getParticipantId()).orElse(null);
            QuizAttempt a = submittedByParticipant.get(join.getParticipantId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("participantId", join.getParticipantId());
            row.put("name", p == null ? "Participant" : p.getName());
            row.put("email", p == null ? "" : p.getEmail());
            row.put("joinedAt", join.getJoinedAt());
            row.put("attemptStatus", a == null ? "NOT_SUBMITTED" : "SUBMITTED");
            row.put("score", a == null ? null : a.getScore());
            row.put("totalQuestions", a == null ? quiz.getQuestions().size() : a.getTotalQuestions());
            row.put("submittedAt", a == null ? null : a.getSubmittedAt());
            rows.add(row);
        }
        return ResponseEntity.ok(Map.of("quizId", quizId, "title", quiz.getTitle(), "participants", rows));
    }

    @GetMapping("/{quizId}/attempts/{attemptId}")
    public ResponseEntity<?> attemptDetails(@PathVariable String quizId, @PathVariable String attemptId, Authentication auth) {
        Quiz quiz = ownedQuiz(quizId, auth.getName());
        if (quiz == null) return ResponseEntity.notFound().build();
        QuizAttempt attempt = attempts.findById(attemptId).orElse(null);
        if (attempt == null || !quizId.equals(attempt.getQuizId())) return ResponseEntity.notFound().build();

        User participant = users.findById(attempt.getParticipantId()).orElse(null);
        List<Map<String, Object>> questionResults = new ArrayList<>();
        Map<Integer, String> answers = attempt.getAnswers() == null ? Map.of() : attempt.getAnswers();
        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            Quiz.Question q = quiz.getQuestions().get(i);
            String given = answers.get(i);
            boolean correct = given != null && q.getCorrectAnswer() != null && q.getCorrectAnswer().trim().equalsIgnoreCase(given.trim());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", i);
            item.put("type", q.getType());
            item.put("question", q.getQuestion());
            item.put("options", q.getOptions());
            item.put("correctAnswer", q.getCorrectAnswer());
            item.put("givenAnswer", given);
            item.put("correct", correct);
            item.put("difficulty", q.getDifficulty());
            item.put("explanation", q.getExplanation());
            item.put("imageUrl", q.getImageUrl());
            questionResults.add(item);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", attempt.getId());
        out.put("quizId", quizId);
        out.put("quizTitle", quiz.getTitle());
        out.put("participantId", attempt.getParticipantId());
        out.put("participantName", participant == null ? "Participant" : participant.getName());
        out.put("participantEmail", participant == null ? "" : participant.getEmail());
        out.put("status", attempt.getStatus());
        out.put("score", attempt.getScore());
        out.put("correctAnswers", attempt.getCorrectAnswers());
        out.put("totalQuestions", attempt.getTotalQuestions());
        out.put("startedAt", attempt.getStartedAt());
        out.put("submittedAt", attempt.getSubmittedAt());
        out.put("durationSeconds", attempt.getDurationSeconds());
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            out.put("timeTakenSeconds", Math.max(0, Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds()));
        } else {
            out.put("timeTakenSeconds", null);
        }
        out.put("answers", answers);
        out.put("activityEvents", attempt.getActivityEvents() == null ? Map.of() : attempt.getActivityEvents());
        out.put("questions", questionResults);
        return ResponseEntity.ok(out);
    }

    private Quiz ownedQuiz(String id, String hostId) {
        Quiz quiz = quizzes.findById(id).orElse(null);
        return quiz != null && hostId.equals(quiz.getHostId()) ? quiz : null;
    }

    private List<QuizAttempt> rankedAttempts(String quizId) {
        List<QuizAttempt> list = new ArrayList<>(attempts.findByQuizIdAndStatusOrderByScoreDescSubmittedAtAsc(quizId, "SUBMITTED"));
        list.sort(Comparator.comparingInt(QuizAttempt::getScore).reversed()
                .thenComparing(Comparator.comparingInt(QuizAttempt::getCorrectAnswers).reversed())
                .thenComparingLong(this::timeTaken)
                .thenComparing(QuizAttempt::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    private long timeTaken(QuizAttempt attempt) {
        if (attempt.getStartedAt() == null || attempt.getSubmittedAt() == null) return Long.MAX_VALUE;
        return Math.max(0, Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds());
    }

    private Map<String, Object> resultRow(Quiz quiz, QuizAttempt attempt, User participant, int rank) {
        int total = attempt.getTotalQuestions();
        int score = attempt.getScore();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rank", rank);
        row.put("attemptId", attempt.getId());
        row.put("participantId", attempt.getParticipantId());
        row.put("name", participant == null ? "Participant" : participant.getName());
        row.put("email", participant == null ? "" : participant.getEmail());
        row.put("score", score);
        row.put("correctAnswers", attempt.getCorrectAnswers());
        row.put("totalQuestions", total);
        int maxScore = quiz.getQuestions().size() * Math.max(1, quiz.getPointsPerCorrect());
        row.put("maxScore", maxScore);
        row.put("percentage", maxScore == 0 ? 0 : Math.max(0, Math.round(score * 10000.0 / maxScore) / 100.0));
        row.put("startedAt", attempt.getStartedAt());
        row.put("submittedAt", attempt.getSubmittedAt());
        row.put("timeTakenSeconds", attempt.getStartedAt() != null && attempt.getSubmittedAt() != null
                ? Math.max(0, Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds()) : null);
        return row;
    }
}
