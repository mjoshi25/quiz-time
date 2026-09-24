package com.quizapp.controller;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizAttempt;
import com.quizapp.repository.QuizAttemptRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.CompetitionLifecycleService;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/quizzes")
public class PublicQuizController {
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final UserRepository users;
    private final CompetitionLifecycleService lifecycle;

    public PublicQuizController(QuizRepository quizzes, QuizAttemptRepository attempts, UserRepository users,
                                CompetitionLifecycleService lifecycle) {
        this.quizzes = quizzes; this.attempts = attempts; this.users = users; this.lifecycle = lifecycle;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        String search = q == null ? "" : q.trim().toLowerCase();
        String categoryFilter = category == null ? "" : category.trim();
        String difficultyFilter = difficulty == null ? "" : difficulty.trim().toUpperCase();
        String statusFilter = status == null ? "" : status.trim().toUpperCase();

        List<Quiz> filtered = quizzes.findAll().stream().map(lifecycle::sync)
                .filter(Objects::nonNull)
                .filter(x -> "PUBLISHED".equals(x.getStatus()) || "STARTED".equals(x.getStatus()) || ("CLOSED".equals(x.getStatus()) && x.isResultPublished()))
                .filter(x -> statusFilter.isBlank() || statusFilter.equals(x.getStatus()))
                .filter(x -> categoryFilter.isBlank() || categoryFilter.equalsIgnoreCase(x.getCategory() == null ? "Other" : x.getCategory()))
                .filter(x -> difficultyFilter.isBlank() || hasDifficulty(x, difficultyFilter))
                .filter(x -> search.isBlank() || contains(x.getTitle(), search) || contains(x.getDescription(), search) || contains(x.getTopic(), search))
                .sorted(comparator(sort))
                .toList();

        int total = filtered.size();
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<PublicQuiz> content = filtered.subList(from, to).stream().map(this::summary).toList();
        return ResponseEntity.ok(new PublicQuizPage(content, safePage, safeSize, total, (int)Math.ceil(total / (double)safeSize)));
    }

    private boolean contains(String value, String search) { return value != null && value.toLowerCase().contains(search); }
    private boolean hasDifficulty(Quiz q, String difficulty) { return q.getQuestions() != null && q.getQuestions().stream().anyMatch(x -> difficulty.equalsIgnoreCase(x.getDifficulty())); }
    private Comparator<Quiz> comparator(String sort) {
        if ("oldest".equalsIgnoreCase(sort)) return Comparator.comparing(Quiz::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("title".equalsIgnoreCase(sort)) return Comparator.comparing(x -> x.getTitle() == null ? "" : x.getTitle(), String.CASE_INSENSITIVE_ORDER);
        if ("questions".equalsIgnoreCase(sort)) return Comparator.comparingInt((Quiz x) -> x.getQuestions() == null ? 0 : x.getQuestions().size()).reversed();
        return Comparator.comparing(Quiz::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Quiz q = quizzes.findById(id).map(lifecycle::sync).orElse(null);
        if (q == null || (!"PUBLISHED".equals(q.getStatus()) && !"STARTED".equals(q.getStatus()) &&
                !("CLOSED".equals(q.getStatus()) && q.isResultPublished()))) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new PublicQuizDetail(
                q.getId(), q.getTitle(), q.getDescription(), q.getCategory(), q.getTopic(), q.getQuestions().size(),
                q.getDurationMinutes(), q.getScheduledStartAt(), q.getScheduledEndAt(), q.getStatus(),
                q.isResultPublished(), q.isCertificateEnabled(), q.getPrizeDescription()
        ));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<?> publicResults(@PathVariable String id) {
        Quiz q = quizzes.findById(id).map(lifecycle::sync).orElse(null);
        if (q == null || !"CLOSED".equals(q.getStatus()) || !q.isResultPublished()) return ResponseEntity.notFound().build();
        List<QuizAttempt> list = rankedAttempts(id);
        List<Map<String,Object>> rows = new ArrayList<>();
        int rank = 0; int previousScore = Integer.MIN_VALUE, previousCorrect = Integer.MIN_VALUE; long previousTime = Long.MIN_VALUE;
        for (int i=0; i<list.size(); i++) {
            QuizAttempt a = list.get(i); long time = timeTaken(a);
            boolean tied = i > 0 && a.getScore() == previousScore && a.getCorrectAnswers() == previousCorrect && time == previousTime;
            if (!tied) rank = i + 1;
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("rank", rank);
            row.put("participantName", users.findById(a.getParticipantId()).map(u -> u.getName()).orElse("Participant"));
            row.put("score", a.getScore()); row.put("correctAnswers", a.getCorrectAnswers());
            row.put("total", a.getTotalQuestions()); row.put("percentage", percentage(q, a.getScore()));
            row.put("timeTakenSeconds", time);
            rows.add(row);
            previousScore=a.getScore(); previousCorrect=a.getCorrectAnswers(); previousTime=time;
        }
        List<Map<String,Object>> winners = rows.stream().filter(r -> ((Integer)r.get("rank")) <= Math.max(1, q.getCertificateTopRanks())).toList();
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("quizId", q.getId());
        result.put("title", q.getTitle());
        result.put("description", q.getDescription()==null?"":q.getDescription());
        result.put("status", q.getStatus());
        result.put("resultPublished", q.isResultPublished());
        result.put("certificateEnabled", q.isCertificateEnabled());
        result.put("certificateTopRanks", q.getCertificateTopRanks());
        result.put("prizeDescription", q.getPrizeDescription()==null?"":q.getPrizeDescription());
        result.put("totalParticipants", rows.size());
        result.put("maxScore", maxScore(q));
        result.put("leaderboard", rows);
        result.put("winners", winners);
        return ResponseEntity.ok(result);
    }

    private PublicQuiz summary(Quiz q) {
        return new PublicQuiz(q.getId(), q.getTitle(), q.getDescription(), q.getCategory(), q.getTopic(), q.getQuestions().size(), q.getDurationMinutes(),
                q.getScheduledStartAt(), q.getScheduledEndAt(), q.getStatus(), q.isResultPublished());
    }

    private List<QuizAttempt> rankedAttempts(String quizId) {
        List<QuizAttempt> list = new ArrayList<>(attempts.findByQuizIdAndStatusOrderByScoreDescSubmittedAtAsc(quizId, "SUBMITTED"));
        list.sort(Comparator.comparingInt(QuizAttempt::getScore).reversed()
                .thenComparing(Comparator.comparingInt(QuizAttempt::getCorrectAnswers).reversed())
                .thenComparingLong(this::timeTaken)
                .thenComparing(QuizAttempt::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }
    private int maxScore(Quiz q) { return q.getQuestions().size() * Math.max(1, q.getPointsPerCorrect()); }
    private double percentage(Quiz q, int score) { int max=maxScore(q); return max<=0?0:Math.max(0, Math.round(score*10000.0/max)/100.0); }
    private long timeTaken(QuizAttempt a) { if(a.getStartedAt()==null||a.getSubmittedAt()==null)return Long.MAX_VALUE; return Math.max(0,Duration.between(a.getStartedAt(),a.getSubmittedAt()).getSeconds()); }

    public record PublicQuiz(String id, String title, String description, String category, String topic, int questionCount, int durationMinutes,
                             java.time.Instant scheduledStartAt, java.time.Instant scheduledEndAt, String status, boolean resultPublished) {}
    public record PublicQuizPage(List<PublicQuiz> content, int page, int size, int totalElements, int totalPages) {}
    public record PublicQuizDetail(String id, String title, String description, String category, String topic, int questionCount, int durationMinutes,
                                   java.time.Instant scheduledStartAt, java.time.Instant scheduledEndAt, String status,
                                   boolean resultPublished, boolean certificateEnabled, String prizeDescription) {}
}
