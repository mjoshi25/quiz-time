package com.quizapp.service;

import com.quizapp.model.Quiz;
import com.quizapp.repository.QuizRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CompetitionLifecycleService {
    private final QuizRepository quizzes;

    public CompetitionLifecycleService(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    /**
     * Synchronizes a quiz's scheduled lifecycle. This is intentionally idempotent,
     * so it is safe to call both from the scheduler and from API requests.
     */
    public Quiz sync(Quiz quiz) {
        if (quiz == null) return null;

        Instant now = Instant.now();
        String status = quiz.getStatus();
        boolean changed = false;

        if ("PUBLISHED".equals(status)) {
            Instant start = quiz.getScheduledStartAt();
            Instant end = quiz.getScheduledEndAt();

            // A scheduled competition becomes live automatically when its start time arrives.
            // If there is no schedule, the host must explicitly press Start Competition.
            if (start != null && !now.isBefore(start)) {
                if (end != null && !now.isBefore(end)) {
                    quiz.setStatus("CLOSED");
                    changed = true;
                } else {
                    quiz.setStatus("STARTED");
                    changed = true;
                }
            }
        } else if ("STARTED".equals(status)) {
            Instant end = quiz.getScheduledEndAt();
            if (end != null && !now.isBefore(end)) {
                quiz.setStatus("CLOSED");
                changed = true;
            }
        }

        if (changed) {
            quiz.setUpdatedAt(now);
            return quizzes.save(quiz);
        }
        return quiz;
    }

    /**
     * Background lifecycle worker. Five seconds gives a responsive automatic start
     * without requiring a participant or host to refresh first.
     */
    @Scheduled(fixedDelay = 5000)
    public void synchronizeScheduledCompetitions() {
        for (Quiz quiz : quizzes.findAll()) {
            sync(quiz);
        }
    }
}
