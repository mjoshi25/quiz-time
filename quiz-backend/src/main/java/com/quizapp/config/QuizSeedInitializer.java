package com.quizapp.config;

import com.quizapp.model.Quiz;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.util.*;

@Configuration
public class QuizSeedInitializer {
    @Bean CommandLineRunner seedQuizSets(QuizRepository quizzes, UserRepository users, @Value("${app.admin.email}") String adminEmail) {
        return args -> {
            String adminId = users.findByEmailIgnoreCase(adminEmail).map(u -> u.getId()).orElse("SYSTEM");
            seed(quizzes, adminId, "General Knowledge Challenge", "10 questions covering geography, science, history, culture and everyday knowledge.", List.of(
                mcq("What is the capital of Australia?", "Canberra", "Sydney", "Melbourne", "Perth"),
                mcq("Which planet is known as the Red Planet?", "Mars", "Venus", "Jupiter", "Mercury"),
                mcq("Who wrote the play Romeo and Juliet?", "William Shakespeare", "Charles Dickens", "Jane Austen", "Mark Twain"),
                mcq("What is the largest ocean on Earth?", "Pacific Ocean", "Atlantic Ocean", "Indian Ocean", "Arctic Ocean"),
                mcq("How many continents are commonly recognized on Earth?", "7", "5", "6", "8"),
                mcq("Which gas do plants primarily absorb during photosynthesis?", "Carbon dioxide", "Oxygen", "Nitrogen", "Hydrogen"),
                mcq("Which instrument is used to measure temperature?", "Thermometer", "Barometer", "Hygrometer", "Anemometer"),
                mcq("Which is the longest river in India?", "Ganga", "Yamuna", "Godavari", "Narmada"),
                mcq("What is the chemical symbol for gold?", "Au", "Ag", "Gd", "Go"),
                tf("The Earth revolves around the Sun.", "True")
            ));
            seed(quizzes, adminId, "Science & Technology Challenge", "10 questions on physics, chemistry, biology, computing and space.", List.of(
                mcq("What is the SI unit of electric current?", "Ampere", "Volt", "Ohm", "Watt"),
                mcq("Which part of a cell contains most of its genetic material?", "Nucleus", "Ribosome", "Cell wall", "Cytoplasm"),
                mcq("What does CPU stand for?", "Central Processing Unit", "Computer Primary Utility", "Central Program User", "Core Processing Utility"),
                mcq("Which force keeps planets in orbit around the Sun?", "Gravity", "Friction", "Magnetism", "Buoyancy"),
                mcq("What is H2O commonly known as?", "Water", "Hydrogen peroxide", "Oxygen", "Salt"),
                mcq("Which device converts digital signals into a form suitable for transmission over communication lines?", "Modem", "Monitor", "Keyboard", "Printer"),
                mcq("Which planet has the largest number of known moons in the Solar System?", "Saturn", "Earth", "Mars", "Mercury"),
                mcq("What type of data structure follows First-In, First-Out?", "Queue", "Stack", "Tree", "Graph"),
                tf("Light travels faster in a vacuum than sound travels through air.", "True"),
                tf("DNA is a type of protein.", "False")
            ));
            seed(quizzes, adminId, "India & World Quiz", "10 questions on Indian civics, geography, history and world facts.", List.of(
                mcq("What is the national currency of India?", "Indian rupee", "Taka", "Riyal", "Dinar"),
                mcq("Which city is known as the Pink City of India?", "Jaipur", "Udaipur", "Jodhpur", "Bikaner"),
                mcq("The Constitution of India came into effect on which date?", "26 January 1950", "15 August 1947", "26 November 1949", "2 October 1950"),
                mcq("Which is the largest Indian state by area?", "Rajasthan", "Madhya Pradesh", "Maharashtra", "Uttar Pradesh"),
                mcq("Which ocean lies to the south of India?", "Indian Ocean", "Pacific Ocean", "Atlantic Ocean", "Arctic Ocean"),
                mcq("Which country is home to the city of Kyoto?", "Japan", "China", "South Korea", "Thailand"),
                mcq("Which organization has its headquarters in New York City and works on international cooperation?", "United Nations", "WTO", "OPEC", "ASEAN"),
                mcq("Which Indian city is the capital of Karnataka?", "Bengaluru", "Mysuru", "Hubballi", "Mangaluru"),
                tf("India is the world's largest democracy by population.", "True"),
                tf("The Sahara is located in South America.", "False")
            ));
        };
    }

    private void seed(QuizRepository repo, String hostId, String title, String description, List<Quiz.Question> questions) {
        if (repo.findByTitle(title).isPresent()) return;
        Quiz q=new Quiz(); q.setTitle(title); q.setDescription(description); q.setHostId(hostId); q.setStatus("STARTED"); q.setDurationMinutes(10); q.setQuestions(questions); q.setUpdatedAt(Instant.now()); repo.save(q);
    }

    private static Quiz.Question mcq(String question,String correct,String... options){
        Quiz.Question q=new Quiz.Question(); q.setType("MCQ"); q.setQuestion(question); q.setOptions(new ArrayList<>(List.of(options))); q.setCorrectAnswer(correct); return q;
    }
    private static Quiz.Question tf(String question,String correct){
        Quiz.Question q=new Quiz.Question(); q.setType("TRUE_FALSE"); q.setQuestion(question); q.setOptions(new ArrayList<>(List.of("True","False"))); q.setCorrectAnswer(correct); return q;
    }
}
