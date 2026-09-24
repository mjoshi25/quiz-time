package com.quizapp.service;

import com.quizapp.model.QuizJoin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class QuizJoinDataInitializer {
    private final MongoTemplate mongoTemplate;
    public QuizJoinDataInitializer(MongoTemplate mongoTemplate) { this.mongoTemplate = mongoTemplate; }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupAndEnsureUniqueJoinIndex() {
        List<QuizJoin> all = mongoTemplate.findAll(QuizJoin.class);
        Map<String, QuizJoin> keep = new HashMap<>();
        for (QuizJoin join : all) {
            String key = String.valueOf(join.getQuizId()) + "::" + String.valueOf(join.getParticipantId());
            QuizJoin current = keep.get(key);
            if (current == null || safeTime(join) < safeTime(current)) keep.put(key, join);
        }
        for (QuizJoin join : all) {
            String key = String.valueOf(join.getQuizId()) + "::" + String.valueOf(join.getParticipantId());
            QuizJoin survivor = keep.get(key);
            if (survivor != null && !survivor.getId().equals(join.getId())) mongoTemplate.remove(join);
        }
        mongoTemplate.indexOps(QuizJoin.class).ensureIndex(
                new Index().on("quizId", Sort.Direction.ASC).on("participantId", Sort.Direction.ASC).unique());
    }
    private long safeTime(QuizJoin join) { return join.getJoinedAt() == null ? Long.MAX_VALUE : join.getJoinedAt().toEpochMilli(); }
}
