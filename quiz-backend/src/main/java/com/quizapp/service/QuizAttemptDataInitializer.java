package com.quizapp.service;

import com.quizapp.model.QuizAttempt;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Repairs duplicate attempt records created by earlier application versions and
 * protects the quizId + participantId relationship from future duplicates.
 */
@Component
public class QuizAttemptDataInitializer {
    private final MongoTemplate mongoTemplate;

    public QuizAttemptDataInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupAndEnsureUniqueAttemptIndex() {
        List<QuizAttempt> all = mongoTemplate.findAll(QuizAttempt.class);
        Map<String, QuizAttempt> keep = new HashMap<>();

        for (QuizAttempt attempt : all) {
            String key = key(attempt);
            QuizAttempt current = keep.get(key);
            if (current == null || preferred(attempt, current)) {
                keep.put(key, attempt);
            }
        }

        for (QuizAttempt attempt : all) {
            QuizAttempt survivor = keep.get(key(attempt));
            if (survivor != null && survivor.getId() != null && !survivor.getId().equals(attempt.getId())) {
                mongoTemplate.remove(attempt);
            }
        }

        mongoTemplate.indexOps(QuizAttempt.class).ensureIndex(
                new Index()
                        .on("quizId", Sort.Direction.ASC)
                        .on("participantId", Sort.Direction.ASC)
                        .unique());
    }

    private String key(QuizAttempt attempt) {
        return String.valueOf(attempt.getQuizId()) + "::" + String.valueOf(attempt.getParticipantId());
    }

    /** Prefer a submitted attempt; otherwise keep the most recently started attempt. */
    private boolean preferred(QuizAttempt candidate, QuizAttempt current) {
        boolean candidateSubmitted = "SUBMITTED".equalsIgnoreCase(candidate.getStatus());
        boolean currentSubmitted = "SUBMITTED".equalsIgnoreCase(current.getStatus());
        if (candidateSubmitted != currentSubmitted) return candidateSubmitted;

        Instant candidateStarted = candidate.getStartedAt();
        Instant currentStarted = current.getStartedAt();
        if (candidateStarted == null) return false;
        if (currentStarted == null) return true;
        return candidateStarted.isAfter(currentStarted);
    }
}
