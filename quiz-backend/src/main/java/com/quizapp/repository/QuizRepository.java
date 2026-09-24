package com.quizapp.repository;

import com.quizapp.model.Quiz;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    List<Quiz> findByCategoryIgnoreCaseOrderByCreatedAtDesc(String category);
    List<Quiz> findByHostIdOrderByCreatedAtDesc(String hostId);
    List<Quiz> findByStatusOrderByCreatedAtDesc(String status); java.util.Optional<Quiz> findByTitle(String title);
}
