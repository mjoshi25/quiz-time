package com.quizapp.controller;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizAttempt;
import com.quizapp.model.User;
import com.quizapp.repository.QuizAttemptRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import java.time.Duration;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participant/profile")
@PreAuthorize("hasRole('PARTICIPANT')")
public class ParticipantProfileController {
    private final UserRepository users;
    private final QuizAttemptRepository attempts;
    private final QuizRepository quizzes;

    public ParticipantProfileController(UserRepository users, QuizAttemptRepository attempts, QuizRepository quizzes) {
        this.users = users; this.attempts = attempts; this.quizzes = quizzes;
    }

    @GetMapping
    public ResponseEntity<?> profile(Authentication auth) {
        User user = users.findById(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        List<QuizAttempt> all = attempts.findByParticipantIdOrderBySubmittedAtDesc(auth.getName());
        List<QuizAttempt> completed = all.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).toList();
        int totalPoints = completed.stream().mapToInt(QuizAttempt::getScore).sum();
        double avg = completed.isEmpty() ? 0 : completed.stream().mapToDouble(a -> percentage(a, quizzes.findById(a.getQuizId()).orElse(null))).average().orElse(0);
        int bestRank = Integer.MAX_VALUE, certificates = 0, perfect = 0, top10 = 0, top3 = 0;
        List<Map<String,Object>> recent = new ArrayList<>();
        Set<String> completedQuizIds = new HashSet<>();

        for (QuizAttempt a : completed) {
            Quiz q = quizzes.findById(a.getQuizId()).orElse(null);
            if (q == null) continue;
            completedQuizIds.add(q.getId());
            List<QuizAttempt> ranked = attempts.findByQuizIdAndStatusOrderByScoreDescSubmittedAtAsc(q.getId(), "SUBMITTED");
            int rank = rankOf(ranked, a.getId());
            if (rank > 0) {
                bestRank = Math.min(bestRank, rank);
                if (rank <= 3) top3++;
                if (rank <= 10) top10++;
                if (q.isCertificateEnabled() && q.isResultPublished() && rank <= Math.max(1, q.getCertificateTopRanks())) certificates++;
            }
            if (percentage(a,q) >= 100) perfect++;
            if (recent.size() < 8) {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("quizId", q.getId()); row.put("title", q.getTitle()); row.put("score", a.getScore());
                row.put("maxScore", q.getQuestions().size() * Math.max(1,q.getPointsPerCorrect()));
                row.put("percentage", round(percentage(a,q))); row.put("rank", rank); row.put("submittedAt", a.getSubmittedAt());
                row.put("timeTakenSeconds", a.getStartedAt()!=null && a.getSubmittedAt()!=null ? Math.max(0,Duration.between(a.getStartedAt(),a.getSubmittedAt()).getSeconds()) : null);
                recent.add(row);
            }
        }

        List<Map<String,Object>> achievements = new ArrayList<>();
        addAchievement(achievements,"FIRST_COMPLETION","First Finish","Completed your first Quizora competition.",completed.size() >= 1);
        addAchievement(achievements,"FIVE_COMPLETIONS","Five Finishes","Completed five competitions.",completed.size() >= 5);
        addAchievement(achievements,"TEN_COMPLETIONS","Ten Finishes","Completed ten competitions.",completed.size() >= 10);
        addAchievement(achievements,"PERFECT_SCORE","Perfect Score","Scored 100% in at least one submitted competition.",perfect > 0);
        addAchievement(achievements,"TOP_THREE","Top Three","Finished in the top three at least once.",top3 > 0);
        addAchievement(achievements,"TOP_TEN","Top Ten","Finished in the top ten at least once.",top10 > 0);
        addAchievement(achievements,"CERTIFICATE","Certificate Earner","Earned at least one enabled competition certificate.",certificates > 0);

        Map<String,Object> out = new LinkedHashMap<>();
        Map<String,Object> profile = new LinkedHashMap<>();
        profile.put("id",user.getId()); profile.put("name",user.getName()); profile.put("email",user.getEmail()); profile.put("role",user.getRole()); profile.put("profilePhoto",user.getProfilePhoto()); profile.put("createdAt",user.getCreatedAt());
        Map<String,Object> stats = new LinkedHashMap<>();
        stats.put("completed",completed.size()); stats.put("totalPoints",totalPoints); stats.put("averagePercentage",round(avg)); stats.put("bestRank",bestRank==Integer.MAX_VALUE?null:bestRank); stats.put("certificates",certificates); stats.put("completedCompetitions",completedQuizIds.size());
        out.put("profile", profile);
        out.put("stats", stats);
        out.put("achievements",achievements); out.put("recent",recent);
        return ResponseEntity.ok(out);
    }

    private static void addAchievement(List<Map<String,Object>> list,String id,String name,String description,boolean unlocked){
        list.add(Map.of("id",id,"name",name,"description",description,"unlocked",unlocked));
    }
    private static int rankOf(List<QuizAttempt> ranked,String id){ for(int i=0;i<ranked.size();i++) if(Objects.equals(ranked.get(i).getId(),id)) return i+1; return 0; }
    private static double percentage(QuizAttempt a, Quiz q){ if(q==null) return 0; int max=q.getQuestions().size()*Math.max(1,q.getPointsPerCorrect()); return max==0?0:Math.max(0,a.getScore()*100.0/max); }
    private static double round(double n){ return Math.round(n*10.0)/10.0; }
}
