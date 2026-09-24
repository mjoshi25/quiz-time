package com.quizapp.controller;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizAttempt;
import com.quizapp.model.QuizJoin;
import com.quizapp.model.User;
import com.quizapp.repository.QuizAttemptRepository;
import com.quizapp.repository.QuizJoinRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.CompetitionLifecycleService;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {
    private final UserRepository users;
    private final QuizRepository quizzes;
    private final QuizJoinRepository joins;
    private final QuizAttemptRepository attempts;
    private final CompetitionLifecycleService lifecycle;

    public AdminAnalyticsController(UserRepository users, QuizRepository quizzes, QuizJoinRepository joins,
                                    QuizAttemptRepository attempts, CompetitionLifecycleService lifecycle) {
        this.users = users; this.quizzes = quizzes; this.joins = joins; this.attempts = attempts; this.lifecycle = lifecycle;
    }

    @GetMapping
    public ResponseEntity<?> overview() {
        List<User> allUsers = users.findAll();
        List<Quiz> allQuizzes = quizzes.findAll().stream().map(lifecycle::sync).toList();
        List<QuizJoin> allJoins = joins.findAll();
        List<QuizAttempt> allAttempts = attempts.findAll();
        Map<String, User> userMap = allUsers.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a,b)->a));
        Map<String, Quiz> quizMap = allQuizzes.stream().collect(Collectors.toMap(Quiz::getId, Function.identity(), (a,b)->a));

        long participants = allUsers.stream().filter(u -> "PARTICIPANT".equals(u.getRole())).count();
        long hosts = allUsers.stream().filter(u -> "HOST".equals(u.getRole())).count();
        long approvedHosts = allUsers.stream().filter(u -> "HOST".equals(u.getRole()) && "APPROVED".equals(u.getHostApprovalStatus())).count();
        long pendingHosts = allUsers.stream().filter(u -> "HOST".equals(u.getRole()) && "PENDING".equals(u.getHostApprovalStatus())).count();
        long suspendedUsers = allUsers.stream().filter(u -> "SUSPENDED".equals(u.getAccountStatus())).count();
        long started = allQuizzes.stream().filter(q -> "STARTED".equals(q.getStatus())).count();
        long published = allQuizzes.stream().filter(q -> "PUBLISHED".equals(q.getStatus())).count();
        long closed = allQuizzes.stream().filter(q -> "CLOSED".equals(q.getStatus())).count();
        long suspendedQuizzes = allQuizzes.stream().filter(q -> "SUSPENDED".equals(q.getStatus())).count();
        long submitted = allAttempts.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count();

        List<Map<String,Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int d=29; d>=0; d--) {
            LocalDate day = today.minusDays(d);
            long newUsers = allUsers.stream().filter(u -> sameDay(u.getCreatedAt(), day)).count();
            long newQuizzes = allQuizzes.stream().filter(q -> sameDay(q.getCreatedAt(), day)).count();
            long joinsCount = allJoins.stream().filter(j -> sameDay(j.getJoinedAt(), day)).count();
            long submissions = allAttempts.stream().filter(a -> "SUBMITTED".equals(a.getStatus()) && sameDay(a.getSubmittedAt(), day)).count();
            trend.add(Map.of("date", day.toString(), "users", newUsers, "quizzes", newQuizzes, "joins", joinsCount, "submissions", submissions));
        }

        List<Map<String,Object>> hostRows = new ArrayList<>();
        for (User host : allUsers.stream().filter(u -> "HOST".equals(u.getRole())).sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(50).toList()) {
            List<Quiz> hq = allQuizzes.stream().filter(q -> Objects.equals(q.getHostId(), host.getId())).toList();
            Set<String> ids = hq.stream().map(Quiz::getId).collect(Collectors.toSet());
            long hjoins = allJoins.stream().filter(j -> ids.contains(j.getQuizId())).count();
            long hattempts = allAttempts.stream().filter(a -> ids.contains(a.getQuizId()) && "SUBMITTED".equals(a.getStatus())).count();
            double avgScore = allAttempts.stream().filter(a -> ids.contains(a.getQuizId()) && "SUBMITTED".equals(a.getStatus())).mapToInt(QuizAttempt::getScore).average().orElse(0);
            hostRows.add(Map.of("id", host.getId(), "name", host.getName()==null?"":host.getName(), "email", host.getEmail()==null?"":host.getEmail(),
                    "approval", host.getHostApprovalStatus()==null?"PENDING":host.getHostApprovalStatus(), "accountStatus", host.getAccountStatus()==null?"ACTIVE":host.getAccountStatus(),
                    "quizCount", hq.size(), "joins", hjoins, "submissions", hattempts, "averageScore", round(avgScore)));
        }

        List<Map<String,Object>> quizRows = allQuizzes.stream().sorted(Comparator.comparing(Quiz::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(50).map(q -> {
            long qjoins = allJoins.stream().filter(j -> Objects.equals(j.getQuizId(), q.getId())).count();
            long qsubs = allAttempts.stream().filter(a -> Objects.equals(a.getQuizId(), q.getId()) && "SUBMITTED".equals(a.getStatus())).count();
            double avg = allAttempts.stream().filter(a -> Objects.equals(a.getQuizId(), q.getId()) && "SUBMITTED".equals(a.getStatus())).mapToInt(QuizAttempt::getScore).average().orElse(0);
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("id", q.getId());
            row.put("title", q.getTitle()==null?"":q.getTitle());
            row.put("hostId", q.getHostId()==null?"":q.getHostId());
            row.put("status", q.getStatus());
            row.put("questions", q.getQuestions()==null?0:q.getQuestions().size());
            row.put("joins", qjoins);
            row.put("submissions", qsubs);
            row.put("averageScore", round(avg));
            row.put("createdAt", q.getCreatedAt());
            return row;
        }).toList();

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("generatedAt", Instant.now());
        Map<String,Object> totals = new LinkedHashMap<>();
        totals.put("users", allUsers.size());
        totals.put("participants", participants);
        totals.put("hosts", hosts);
        totals.put("approvedHosts", approvedHosts);
        totals.put("pendingHosts", pendingHosts);
        totals.put("suspendedUsers", suspendedUsers);
        totals.put("quizzes", allQuizzes.size());
        totals.put("publishedQuizzes", published);
        totals.put("startedQuizzes", started);
        totals.put("closedQuizzes", closed);
        totals.put("suspendedQuizzes", suspendedQuizzes);
        totals.put("joins", allJoins.size());
        totals.put("submittedAttempts", submitted);
        out.put("totals", totals);
        out.put("trend", trend); out.put("hosts", hostRows); out.put("quizzes", quizRows);
        return ResponseEntity.ok(out);
    }

    @GetMapping(value="/export", produces="text/csv")
    public ResponseEntity<String> export() {
        ResponseEntity<?> response = overview();
        @SuppressWarnings("unchecked") Map<String,Object> data = (Map<String,Object>) response.getBody();
        StringBuilder csv = new StringBuilder("Report,Value\n");
        Map<String,Object> totals = (Map<String,Object>) data.get("totals");
        totals.forEach((k,v) -> csv.append(csvCell(k)).append(',').append(csvCell(String.valueOf(v))).append('\n'));
        csv.append("\nDate,Users,Quizzes,Joins,Submissions\n");
        for (Map<String,Object> r : (List<Map<String,Object>>)data.get("trend")) csv.append(r.get("date")).append(',').append(r.get("users")).append(',').append(r.get("quizzes")).append(',').append(r.get("joins")).append(',').append(r.get("submissions")).append('\n');
        csv.append("\nHost,Email,Approval,Quizzes,Joins,Submissions,Average Score\n");
        for (Map<String,Object> r : (List<Map<String,Object>>)data.get("hosts")) csv.append(csvCell(String.valueOf(r.get("name")))).append(',').append(csvCell(String.valueOf(r.get("email")))).append(',').append(r.get("approval")).append(',').append(r.get("quizCount")).append(',').append(r.get("joins")).append(',').append(r.get("submissions")).append(',').append(r.get("averageScore")).append('\n');
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=quizora-platform-analytics.csv").body(csv.toString());
    }


    @GetMapping("/range")
    public ResponseEntity<?> range(@RequestParam String from, @RequestParam String to) {
        try {
            LocalDate start=LocalDate.parse(from), end=LocalDate.parse(to);
            if(end.isBefore(start)) return ResponseEntity.badRequest().body("to must be on or after from");
            Instant f=start.atStartOfDay(ZoneOffset.UTC).toInstant(); Instant t=end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            List<User> us=users.findAll(); List<Quiz> qs=quizzes.findAll().stream().map(lifecycle::sync).toList(); List<QuizJoin> js=joins.findAll(); List<QuizAttempt> as=attempts.findAll();
            long newUsers=us.stream().filter(u->between(u.getCreatedAt(),f,t)).count();
            long newQuizzes=qs.stream().filter(q->between(q.getCreatedAt(),f,t)).count();
            long rangeJoins=js.stream().filter(j->between(j.getJoinedAt(),f,t)).count();
            long submissions=as.stream().filter(a->"SUBMITTED".equals(a.getStatus())&&between(a.getSubmittedAt(),f,t)).count();
            double avg=as.stream().filter(a->"SUBMITTED".equals(a.getStatus())&&between(a.getSubmittedAt(),f,t)).mapToInt(QuizAttempt::getScore).average().orElse(0);
            List<Map<String,Object>> quizRows=qs.stream().filter(q->between(q.getCreatedAt(),f,t)).sorted(Comparator.comparing(Quiz::getCreatedAt,Comparator.nullsLast(Comparator.reverseOrder()))).map(q->{
                long j=js.stream().filter(x->Objects.equals(x.getQuizId(),q.getId())&&between(x.getJoinedAt(),f,t)).count();
                long sub=as.stream().filter(x->Objects.equals(x.getQuizId(),q.getId())&&"SUBMITTED".equals(x.getStatus())&&between(x.getSubmittedAt(),f,t)).count();
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("id", q.getId());
                row.put("title", q.getTitle()==null?"":q.getTitle());
                row.put("status", q.getStatus());
                row.put("joins", j);
                row.put("submissions", sub);
                row.put("createdAt", q.getCreatedAt());
                return row;
            }).toList();
            return ResponseEntity.ok(Map.of("from",from,"to",to,"newUsers",newUsers,"newQuizzes",newQuizzes,"joins",rangeJoins,"submissions",submissions,"averageScore",round(avg),"quizzes",quizRows));
        } catch(Exception e){ return ResponseEntity.badRequest().body("Dates must use YYYY-MM-DD"); }
    }

    private boolean between(Instant v,Instant f,Instant t){return v!=null&&!v.isBefore(f)&&v.isBefore(t);}
    private boolean sameDay(Instant value, LocalDate day){ return value != null && value.atZone(ZoneOffset.UTC).toLocalDate().equals(day); }
    private double round(double v){ return Math.round(v*100.0)/100.0; }
    private String csvCell(String s){ if(s==null)return ""; return "\""+s.replace("\"","\"\"")+"\""; }
}
