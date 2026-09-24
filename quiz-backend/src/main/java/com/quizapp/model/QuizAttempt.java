package com.quizapp.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("quiz_attempts")
public class QuizAttempt {
    @Id private String id;
    private String quizId;
    private String participantId;
    private Instant startedAt;
    private Instant submittedAt;
    private String status = "IN_PROGRESS";
    private Map<Integer,String> answers = new HashMap<>();
    private int score;
    private int totalQuestions;
    private int correctAnswers;
    private int durationSeconds;
    private Map<String,Integer> activityEvents = new HashMap<>();
    private Map<Integer,Integer> questionTimeSeconds = new HashMap<>();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getQuizId(){return quizId;} public void setQuizId(String v){quizId=v;}
    public String getParticipantId(){return participantId;} public void setParticipantId(String v){participantId=v;}
    public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;}
    public Instant getSubmittedAt(){return submittedAt;} public void setSubmittedAt(Instant v){submittedAt=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Map<Integer,String> getAnswers(){return answers;} public void setAnswers(Map<Integer,String> v){answers=v;}
    public int getScore(){return score;} public void setScore(int v){score=v;}
    public int getTotalQuestions(){return totalQuestions;} public void setTotalQuestions(int v){totalQuestions=v;}
    public int getCorrectAnswers(){return correctAnswers;} public void setCorrectAnswers(int v){correctAnswers=v;}
    public int getDurationSeconds(){return durationSeconds;} public void setDurationSeconds(int v){durationSeconds=v;}
    public Map<String,Integer> getActivityEvents(){return activityEvents;} public void setActivityEvents(Map<String,Integer> v){activityEvents=v;}
    public Map<Integer,Integer> getQuestionTimeSeconds(){return questionTimeSeconds;} public void setQuestionTimeSeconds(Map<Integer,Integer> v){questionTimeSeconds=v;}
}
