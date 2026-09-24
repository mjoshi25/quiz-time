package com.quizapp.repository;

import com.quizapp.model.QuizAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuizAttemptRepository extends MongoRepository<QuizAttempt,String> {
    Optional<QuizAttempt> findTopByQuizIdAndParticipantIdOrderByStartedAtDesc(String quizId, String participantId);
    List<QuizAttempt> findByQuizIdAndStatusOrderByScoreDescSubmittedAtAsc(String quizId, String status);
    List<QuizAttempt> findByParticipantIdOrderBySubmittedAtDesc(String participantId);
    long countByQuizIdAndParticipantIdAndStatus(String quizId, String participantId, String status);
}
