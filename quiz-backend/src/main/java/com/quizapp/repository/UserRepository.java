package com.quizapp.repository;import java.util.*;import org.springframework.data.mongodb.repository.MongoRepository;import com.quizapp.model.User;
public interface UserRepository extends MongoRepository<User,String>{Optional<User> findByEmailIgnoreCase(String email);List<User> findByRoleAndHostApprovalStatus(String role,String status); List<User> findByRoleOrderByCreatedAtDesc(String role);}
