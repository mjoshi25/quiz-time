package com.quizapp.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;import org.springframework.data.mongodb.core.index.Indexed;

@Document("quizzes")
public class Quiz {
    @Id private String id;
    private String title;
    private String description;
    @Indexed private String category = "Other";
    private String topic = "";
    @Indexed private String hostId;
    @Indexed private String status = "DRAFT";
    private String statusBeforeSuspension;
    private int durationMinutes = 10;
    private int pointsPerCorrect = 1;
    private boolean negativeMarkingEnabled = false;
    private int penaltyPerWrong = 0;
    private boolean allowRetake = false;
    private int maxAttempts = 1;
    private boolean resultPublished = true;
    private boolean certificateEnabled = false;
    private int certificateTopRanks = 3;
    private String prizeDescription = "";
    private Instant scheduledStartAt;
    private Instant scheduledEndAt;
    private List<Question> questions = new ArrayList<>();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getTopic(){return topic;} public void setTopic(String v){topic=v;}
    public String getHostId(){return hostId;} public void setHostId(String v){hostId=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getStatusBeforeSuspension(){return statusBeforeSuspension;} public void setStatusBeforeSuspension(String v){statusBeforeSuspension=v;}
    public int getDurationMinutes(){return durationMinutes;} public void setDurationMinutes(int v){durationMinutes=v;}
    public int getPointsPerCorrect(){return pointsPerCorrect;} public void setPointsPerCorrect(int v){pointsPerCorrect=v;}
    public boolean isNegativeMarkingEnabled(){return negativeMarkingEnabled;} public void setNegativeMarkingEnabled(boolean v){negativeMarkingEnabled=v;}
    public int getPenaltyPerWrong(){return penaltyPerWrong;} public void setPenaltyPerWrong(int v){penaltyPerWrong=v;}
    public boolean isAllowRetake(){return allowRetake;} public void setAllowRetake(boolean v){allowRetake=v;}
    public int getMaxAttempts(){return maxAttempts;} public void setMaxAttempts(int v){maxAttempts=v;}
    public boolean isResultPublished(){return resultPublished;} public void setResultPublished(boolean v){resultPublished=v;}
    public boolean isCertificateEnabled(){return certificateEnabled;} public void setCertificateEnabled(boolean v){certificateEnabled=v;}
    public int getCertificateTopRanks(){return certificateTopRanks;} public void setCertificateTopRanks(int v){certificateTopRanks=v;}
    public String getPrizeDescription(){return prizeDescription;} public void setPrizeDescription(String v){prizeDescription=v;}
    public Instant getScheduledStartAt(){return scheduledStartAt;} public void setScheduledStartAt(Instant v){scheduledStartAt=v;}
    public Instant getScheduledEndAt(){return scheduledEndAt;} public void setScheduledEndAt(Instant v){scheduledEndAt=v;}
    public List<Question> getQuestions(){return questions;} public void setQuestions(List<Question> v){questions=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(Instant v){updatedAt=v;}

    public static class Question {
        private String type;
        private String question;
        private List<String> options = new ArrayList<>();
        private String correctAnswer;
        private String difficulty = "MEDIUM";
        private String explanation = "";
        private String imageUrl = "";
        public String getType(){return type;} public void setType(String v){type=v;}
        public String getQuestion(){return question;} public void setQuestion(String v){question=v;}
        public List<String> getOptions(){return options;} public void setOptions(List<String> v){options=v;}
        public String getCorrectAnswer(){return correctAnswer;} public void setCorrectAnswer(String v){correctAnswer=v;}
        public String getDifficulty(){return difficulty;} public void setDifficulty(String v){difficulty=v;}
        public String getExplanation(){return explanation;} public void setExplanation(String v){explanation=v;}
        public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
    }
}
