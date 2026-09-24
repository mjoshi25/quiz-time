package com.quizapp.repository;

import com.quizapp.model.QuestionBankItem;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuestionBankRepository extends MongoRepository<QuestionBankItem, String> {
    List<QuestionBankItem> findByHostIdOrderByCreatedAtDesc(String hostId);
}
