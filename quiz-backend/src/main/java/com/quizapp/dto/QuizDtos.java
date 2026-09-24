package com.quizapp.dto;

import java.time.Instant;
import java.util.List;

public class QuizDtos {
    public record QuizRequest(String title, String description, List<QuestionRequest> questions, Integer durationMinutes, Instant scheduledStartAt, Instant scheduledEndAt, Integer pointsPerCorrect, Boolean negativeMarkingEnabled, Integer penaltyPerWrong, Boolean allowRetake, Integer maxAttempts, Boolean resultPublished, Boolean certificateEnabled, Integer certificateTopRanks, String prizeDescription, String category, String topic) {}
    public record QuestionRequest(String type, String question, List<String> options, String correctAnswer, String difficulty, String explanation, String imageUrl) {}
}
