package com.quizapp.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("question_bank")
public class QuestionBankItem {
    @Id private String id;
    private String hostId;
    private String type = "MCQ";
    private String question;
    private List<String> options = new ArrayList<>();
    private String correctAnswer;
    private String difficulty = "MEDIUM";
    private String explanation = "";
    private String imageUrl = "";
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getHostId(){return hostId;} public void setHostId(String v){hostId=v;}
    public String getType(){return type;} public void setType(String v){type=v;}
    public String getQuestion(){return question;} public void setQuestion(String v){question=v;}
    public List<String> getOptions(){return options;} public void setOptions(List<String> v){options=v;}
    public String getCorrectAnswer(){return correctAnswer;} public void setCorrectAnswer(String v){correctAnswer=v;}
    public String getDifficulty(){return difficulty;} public void setDifficulty(String v){difficulty=v;}
    public String getExplanation(){return explanation;} public void setExplanation(String v){explanation=v;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
