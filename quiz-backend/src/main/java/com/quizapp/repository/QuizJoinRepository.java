package com.quizapp.repository;

import com.quizapp.model.QuizJoin;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuizJoinRepository extends MongoRepository<QuizJoin, String> {
    // Older database versions may contain duplicate join records. Returning the newest
    // record avoids Mongo's IncorrectResultSizeDataAccessException and keeps the join idempotent.
    Optional<QuizJoin> findTopByQuizIdAndParticipantIdOrderByJoinedAtDesc(String quizId, String participantId);
    List<QuizJoin> findByQuizId(String quizId);
    List<QuizJoin> findByParticipantIdOrderByJoinedAtDesc(String participantId);
    long countByQuizId(String quizId);
    void deleteByQuizId(String quizId);
}
