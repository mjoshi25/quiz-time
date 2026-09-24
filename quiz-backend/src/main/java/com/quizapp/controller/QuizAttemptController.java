package com.quizapp.controller;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizAttempt;
import com.quizapp.repository.QuizAttemptRepository;
import com.quizapp.repository.QuizJoinRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.CompetitionLifecycleService;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participant/attempts")
@PreAuthorize("hasRole('PARTICIPANT')")
public class QuizAttemptController {
    private final QuizRepository quizzes;
    private final QuizJoinRepository joins;
    private final QuizAttemptRepository attempts;
    private final com.quizapp.service.NotificationService notifications;
    private final UserRepository users;
    private final CompetitionLifecycleService lifecycle;
    public QuizAttemptController(QuizRepository quizzes, QuizJoinRepository joins, QuizAttemptRepository attempts, CompetitionLifecycleService lifecycle, com.quizapp.service.NotificationService notifications, UserRepository users){this.quizzes=quizzes;this.joins=joins;this.attempts=attempts;this.lifecycle=lifecycle; this.notifications=notifications; this.users=users;}

    @PostMapping("/{quizId}/start")
    public ResponseEntity<?> start(@PathVariable String quizId, Authentication auth){
        Quiz q=quizzes.findById(quizId).orElse(null);
        q = lifecycle.sync(q);
        if(q==null || !"STARTED".equals(q.getStatus())) return ResponseEntity.status(409).body("The competition has not been started by the host.");
        Instant now = Instant.now();
        if (q.getScheduledStartAt() != null && now.isBefore(q.getScheduledStartAt())) return ResponseEntity.status(409).body("This competition has not started yet.");
        if (q.getScheduledEndAt() != null && !now.isBefore(q.getScheduledEndAt())) return ResponseEntity.status(409).body("This competition has ended.");
        if(joins.findTopByQuizIdAndParticipantIdOrderByJoinedAtDesc(quizId,auth.getName()).isEmpty()) return ResponseEntity.status(403).body("Join the quiz before starting it.");
        Optional<QuizAttempt> existing=attempts.findTopByQuizIdAndParticipantIdOrderByStartedAtDesc(quizId,auth.getName());
        if(existing.isPresent()){
            QuizAttempt a=existing.get();
            if(!"SUBMITTED".equals(a.getStatus())) return ResponseEntity.ok(publicAttempt(q,a));
            long completed = attempts.countByQuizIdAndParticipantIdAndStatus(quizId, auth.getName(), "SUBMITTED");
            if(!q.isAllowRetake() || completed >= Math.max(1, q.getMaxAttempts())) return ResponseEntity.ok(publicResult(q,a));
        }
        QuizAttempt a=new QuizAttempt(); a.setQuizId(quizId); a.setParticipantId(auth.getName()); a.setStartedAt(Instant.now()); a.setTotalQuestions(q.getQuestions().size()); a.setDurationSeconds(Math.max(60, q.getDurationMinutes() * 60)); attempts.save(a);
        return ResponseEntity.ok(publicAttempt(q,a));
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<?> get(@PathVariable String quizId, Authentication auth){
        Quiz q=quizzes.findById(quizId).orElse(null); if(q==null)return ResponseEntity.notFound().build();
        q = lifecycle.sync(q);
        QuizAttempt a=attempts.findTopByQuizIdAndParticipantIdOrderByStartedAtDesc(quizId,auth.getName()).orElse(null); if(a==null)return ResponseEntity.notFound().build();
        return ResponseEntity.ok("SUBMITTED".equals(a.getStatus())?publicResult(q,a):publicAttempt(q,a));
    }

    @PostMapping("/{quizId}/submit")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<?> submit(@PathVariable String quizId,@RequestBody SubmitRequest req,Authentication auth){
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required. Please sign in again.");
        }
        Quiz q=quizzes.findById(quizId).orElse(null); if(q==null)return ResponseEntity.notFound().build();
        q = lifecycle.sync(q);
        QuizAttempt a=attempts.findTopByQuizIdAndParticipantIdOrderByStartedAtDesc(quizId,auth.getName()).orElse(null); if(a==null)return ResponseEntity.badRequest().body("Start the quiz first.");
        if("SUBMITTED".equals(a.getStatus())) return ResponseEntity.ok(publicResult(q,a));
        Map<Integer,String> answers=req==null||req.answers()==null?new HashMap<>():new HashMap<>(req.answers());
        if(req!=null && req.questionTimeSeconds()!=null) a.setQuestionTimeSeconds(new HashMap<>(req.questionTimeSeconds()));
        boolean expired=Duration.between(a.getStartedAt(),Instant.now()).getSeconds()>a.getDurationSeconds();
        int correct=0;
        for(int i=0;i<q.getQuestions().size();i++){
            String given=answers.get(i); String expected=q.getQuestions().get(i).getCorrectAnswer();
            if(given!=null && expected!=null && expected.trim().equalsIgnoreCase(given.trim())) correct++;
        }
        return finalizeAttempt(q,a,answers,expired);
    }

    @PatchMapping("/{quizId}/save")
    public ResponseEntity<?> save(@PathVariable String quizId, @RequestBody SubmitRequest req, Authentication auth){
        Quiz q=quizzes.findById(quizId).orElse(null); if(q==null)return ResponseEntity.notFound().build();
        QuizAttempt a=attempts.findTopByQuizIdAndParticipantIdOrderByStartedAtDesc(quizId,auth.getName()).orElse(null);
        if(a==null)return ResponseEntity.badRequest().body("Start the quiz first.");
        if("SUBMITTED".equals(a.getStatus())) return ResponseEntity.ok(publicResult(q,a));
        long elapsed=Duration.between(a.getStartedAt(),Instant.now()).getSeconds();
        if(elapsed>=a.getDurationSeconds()) return finalizeAttempt(q,a,a.getAnswers(),true);
        if(req!=null && req.answers()!=null) a.setAnswers(new HashMap<>(req.answers()));
        if(req!=null && req.questionTimeSeconds()!=null) a.setQuestionTimeSeconds(new HashMap<>(req.questionTimeSeconds()));
        attempts.save(a);
        return ResponseEntity.ok(Map.of("saved",true,"remainingSeconds",Math.max(0,a.getDurationSeconds()-elapsed)));
    }

    @PostMapping("/{quizId}/activity")
    public ResponseEntity<?> activity(@PathVariable String quizId,@RequestBody ActivityRequest req,Authentication auth){
        QuizAttempt a=attempts.findTopByQuizIdAndParticipantIdOrderByStartedAtDesc(quizId,auth.getName()).orElse(null);
        if(a==null || "SUBMITTED".equals(a.getStatus())) return ResponseEntity.ok(Map.of("recorded",false));
        Map<String,Integer> events=a.getActivityEvents()==null?new HashMap<>():new HashMap<>(a.getActivityEvents());
        String type=req==null?null:req.type();
        if(type!=null && !type.isBlank()) events.put(type,events.getOrDefault(type,0)+1);
        a.setActivityEvents(events); attempts.save(a);
        return ResponseEntity.ok(Map.of("recorded",true,"events",events));
    }

    @GetMapping("/{quizId}/leaderboard")
    public ResponseEntity<?> leaderboard(@PathVariable String quizId, Authentication auth){
        Quiz q=quizzes.findById(quizId).orElse(null); if(q==null)return ResponseEntity.notFound().build();
        List<QuizAttempt> list=rankedAttempts(quizId);
        List<Map<String,Object>> out=new ArrayList<>();
        int rank=0; int previousScore=-1; int previousCorrect=-1; long previousTime=-1;
        for(int i=0;i<list.size();i++){
            QuizAttempt a=list.get(i);
            long timeTaken=timeTaken(a);
            boolean sameRank=i>0 && a.getScore()==previousScore && a.getCorrectAnswers()==previousCorrect && timeTaken==previousTime;
            if(!sameRank) rank=i+1;
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("rank",rank); row.put("participantId",a.getParticipantId()); row.put("participantName",users.findById(a.getParticipantId()).map(u->u.getName()).orElse("Participant")); row.put("score",a.getScore());
            row.put("correctAnswers",a.getCorrectAnswers()); row.put("total",a.getTotalQuestions());
            row.put("percentage",percentage(q, a.getScore()));
            row.put("timeTakenSeconds",timeTaken); row.put("submittedAt",a.getSubmittedAt());
            out.add(row);
            previousScore=a.getScore(); previousCorrect=a.getCorrectAnswers(); previousTime=timeTaken;
        }
        Map<String,Object> response=new LinkedHashMap<>();
        response.put("quizId",quizId); response.put("title",q.getTitle()); response.put("pointsPerCorrect",q.getPointsPerCorrect()); response.put("negativeMarkingEnabled",q.isNegativeMarkingEnabled()); response.put("penaltyPerWrong",q.getPenaltyPerWrong()); response.put("maxScore",maxScore(q)); response.put("totalParticipants",out.size());
        response.put("leaderboard",out);
        if(auth!=null){
            Optional<QuizAttempt> mine=list.stream().filter(a->auth.getName().equals(a.getParticipantId())).findFirst();
            if(mine.isPresent()){
                int index=list.indexOf(mine.get());
                Map<String,Object> own=new LinkedHashMap<>();
                own.put("rank",index>=0?(int)out.get(index).get("rank"):null); own.put("participantName",users.findById(mine.get().getParticipantId()).map(u->u.getName()).orElse("Participant")); own.put("score",mine.get().getScore());
                own.put("correctAnswers",mine.get().getCorrectAnswers()); own.put("total",mine.get().getTotalQuestions()); own.put("maxScore",maxScore(q));
                own.put("percentage",percentage(q, mine.get().getScore()));
                own.put("timeTakenSeconds",timeTaken(mine.get()));
                own.put("percentile",out.isEmpty()?0:Math.round((out.size()-index-1)*10000.0/out.size())/100.0);
                response.put("myStanding",own);
            }
        }
        return ResponseEntity.ok(response);
    }

    private List<QuizAttempt> rankedAttempts(String quizId){
        List<QuizAttempt> list=new ArrayList<>(attempts.findByQuizIdAndStatusOrderByScoreDescSubmittedAtAsc(quizId,"SUBMITTED"));
        list.sort(Comparator.comparingInt(QuizAttempt::getScore).reversed()
                .thenComparing(Comparator.comparingInt(QuizAttempt::getCorrectAnswers).reversed())
                .thenComparingLong(this::timeTaken)
                .thenComparing(QuizAttempt::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    private int maxScore(Quiz q){ return q.getQuestions().size() * Math.max(1, q.getPointsPerCorrect()); }

    private double percentage(Quiz q, int score){
        int max = maxScore(q);
        if(max <= 0) return 0;
        return Math.max(0, Math.round(score * 10000.0 / max) / 100.0);
    }

    private long timeTaken(QuizAttempt a){
        if(a.getStartedAt()==null || a.getSubmittedAt()==null) return Long.MAX_VALUE;
        return Math.max(0,Duration.between(a.getStartedAt(),a.getSubmittedAt()).getSeconds());
    }

    private ResponseEntity<?> finalizeAttempt(Quiz q, QuizAttempt a, Map<Integer,String> answers, boolean expired){
        if(answers==null) answers=new HashMap<>();
        int correct=0;
        int wrong=0;
        for(int i=0;i<q.getQuestions().size();i++){
            String given=answers.get(i); String expected=q.getQuestions().get(i).getCorrectAnswer();
            if(given==null || given.trim().isEmpty()) continue;
            if(expected!=null && expected.trim().equalsIgnoreCase(given.trim())) correct++; else wrong++;
        }
        int points = Math.max(1, q.getPointsPerCorrect());
        int penalty = q.isNegativeMarkingEnabled() ? Math.max(0, q.getPenaltyPerWrong()) : 0;
        int score = (correct * points) - (wrong * penalty);
        a.setAnswers(new HashMap<>(answers)); a.setCorrectAnswers(correct); a.setScore(score); a.setStatus("SUBMITTED"); a.setSubmittedAt(Instant.now()); attempts.save(a);
        notifications.send(a.getParticipantId(), "Quiz result available", "Your result for \""+q.getTitle()+"\" is now available. Score: "+score+"/"+maxScore(q)+".", "RESULT", "/quiz/"+q.getId()+"/result");
        notifications.send(q.getHostId(), "Participant submitted", "A participant has submitted \""+q.getTitle()+"\". Open Results to review the latest ranking.", "SUBMISSION", "/host/quizzes/"+q.getId()+"/results");
        return ResponseEntity.ok(publicResult(q,a,expired));
    }

    private Map<String,Object> publicAttempt(Quiz q,QuizAttempt a){
        List<Map<String,Object>> questions=new ArrayList<>();
        for(int i=0;i<q.getQuestions().size();i++){Quiz.Question x=q.getQuestions().get(i); Map<String,Object> m=new LinkedHashMap<>();m.put("index",i);m.put("type",x.getType());m.put("question",x.getQuestion());m.put("options",x.getOptions());m.put("difficulty",x.getDifficulty());m.put("imageUrl",x.getImageUrl());questions.add(m);}
        long elapsed=Math.max(0,Duration.between(a.getStartedAt(),Instant.now()).getSeconds()); long remaining=Math.max(0,a.getDurationSeconds()-elapsed);
        return Map.of("attemptId",a.getId(),"quizId",q.getId(),"title",q.getTitle(),"description",q.getDescription(),"startedAt",a.getStartedAt(),"durationSeconds",a.getDurationSeconds(),"remainingSeconds",remaining,"questions",questions,"answers",a.getAnswers());
    }
    private Map<String,Object> publicResult(Quiz q,QuizAttempt a){return publicResult(q,a,false);}
    private Map<String,Object> publicResult(Quiz q,QuizAttempt a,boolean expired){
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("attemptId",a.getId()); out.put("quizId",q.getId()); out.put("title",q.getTitle());
        out.put("status",a.getStatus()); out.put("score",a.getScore()); out.put("correctAnswers",a.getCorrectAnswers());
        out.put("totalQuestions",a.getTotalQuestions()); out.put("maxScore",maxScore(q)); out.put("pointsPerCorrect",q.getPointsPerCorrect()); out.put("negativeMarkingEnabled",q.isNegativeMarkingEnabled()); out.put("penaltyPerWrong",q.getPenaltyPerWrong()); out.put("submittedAt",a.getSubmittedAt()); out.put("expired",expired); out.put("answers",a.getAnswers());
        out.put("questionTimeSeconds",a.getQuestionTimeSeconds()==null?Map.of():a.getQuestionTimeSeconds());
        out.put("allowRetake",q.isAllowRetake()); out.put("maxAttempts",q.getMaxAttempts());
        out.put("resultPublished", q.isResultPublished()); out.put("certificateEnabled", q.isCertificateEnabled()); out.put("certificateTopRanks", q.getCertificateTopRanks()); out.put("prizeDescription", q.getPrizeDescription());
        out.put("attemptsUsed",attempts.countByQuizIdAndParticipantIdAndStatus(q.getId(),a.getParticipantId(),"SUBMITTED"));
        List<Map<String,Object>> review = new ArrayList<>();
        Map<Integer,String> answers = a.getAnswers() == null ? Map.of() : a.getAnswers();
        for (int i=0;i<q.getQuestions().size();i++) {
            Quiz.Question item=q.getQuestions().get(i);
            String given=answers.get(i);
            boolean correct=given!=null && item.getCorrectAnswer()!=null && item.getCorrectAnswer().trim().equalsIgnoreCase(given.trim());
            Map<String,Object> r=new LinkedHashMap<>(); r.put("index",i); r.put("question",item.getQuestion()); r.put("givenAnswer",given);
            r.put("correctAnswer",item.getCorrectAnswer()); r.put("correct",correct); r.put("explanation",item.getExplanation()); r.put("imageUrl",item.getImageUrl()); r.put("difficulty",item.getDifficulty()); r.put("type",item.getType());
            review.add(r);
        }
        out.put("review",review);
        return out;
    }
    public record SubmitRequest(Map<Integer,String> answers, Map<Integer,Integer> questionTimeSeconds){}
    public record ActivityRequest(String type){}
}
