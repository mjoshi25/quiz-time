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
            seedClassOneMath(quizzes, users);
        };
    }

    private void seedClassOneMath(QuizRepository quizzes, UserRepository users) {
        final String title = "Class 1 Mathematics - Basics (30 Questions)";
        var host = users.findByEmailIgnoreCase("host1@quiztime.com")
                .filter(u -> "HOST".equalsIgnoreCase(u.getRole()))
                .orElse(null);
        if (host == null) {
            System.out.println("Quizora Class 1 Mathematics seed skipped: HOST account host1@quiztime.com was not found. Create/register that host account, then restart the backend.");
            return;
        }
        boolean alreadySeeded = quizzes.findByHostIdOrderByCreatedAtDesc(host.getId()).stream()
                .anyMatch(q -> title.equals(q.getTitle()));
        if (alreadySeeded) return;

        List<Quiz.Question> questions = List.of(
            math("What is 2 + 3?", "5", "Add two and three to get five.", "5", "4", "6", "7"),
            math("What is 4 + 1?", "5", "Four plus one more makes five.", "5", "3", "6", "8"),
            math("What is 6 + 2?", "8", "Six objects and two more make eight.", "8", "7", "9", "6"),
            math("What is 5 + 5?", "10", "Five plus five equals ten.", "10", "9", "11", "12"),
            math("What is 7 + 3?", "10", "Seven plus three makes ten.", "10", "8", "9", "13"),
            math("What is 9 + 2?", "11", "Count two more after nine: ten, eleven.", "11", "10", "12", "13"),
            math("What is 8 + 4?", "12", "Eight plus four equals twelve.", "12", "11", "13", "14"),
            math("What is 10 + 5?", "15", "Ten plus five is fifteen.", "15", "14", "16", "12"),
            math("What is 6 + 6?", "12", "Two groups of six make twelve.", "12", "10", "11", "13"),
            math("Riya has 3 apples and gets 2 more. How many apples does she have?", "5", "Three apples plus two apples equals five apples.", "5", "4", "6", "7"),
            math("What is 5 - 2?", "3", "Take two away from five; three remain.", "3", "2", "4", "7"),
            math("What is 8 - 3?", "5", "Eight take away three leaves five.", "5", "4", "6", "3"),
            math("What is 10 - 4?", "6", "Ten take away four equals six.", "6", "5", "7", "4"),
            math("What is 9 - 1?", "8", "One less than nine is eight.", "8", "7", "9", "6"),
            math("What is 12 - 2?", "10", "Twelve take away two equals ten.", "10", "9", "11", "8"),
            math("What is 15 - 5?", "10", "Fifteen take away five leaves ten.", "10", "5", "15", "9"),
            math("What is 7 - 4?", "3", "Seven take away four leaves three.", "3", "2", "4", "5"),
            math("There are 6 birds. Two fly away. How many are left?", "4", "Six birds minus two birds equals four birds.", "4", "3", "5", "8"),
            math("Which number comes after 6?", "7", "Seven comes immediately after six.", "7", "5", "8", "9"),
            math("Which number comes before 10?", "9", "Nine comes immediately before ten.", "9", "8", "10", "11"),
            math("Fill in the missing number: 3, 4, __, 6.", "5", "The numbers go up by one: three, four, five, six.", "5", "2", "7", "8"),
            math("Which is the greatest number?", "9", "Nine is greater than four, six and two.", "9", "4", "6", "2"),
            math("Which is the smallest number?", "1", "One is smaller than three, five and seven.", "1", "3", "5", "7"),
            math("Which sign makes this true: 4 __ 7?", "<", "Four is less than seven, so use the less-than sign.", "<", ">", "=", "+"),
            math("Which sign makes this true: 8 __ 3?", ">", "Eight is greater than three, so use the greater-than sign.", ">", "<", "=", "-"),
            math("How many tens are there in 14?", "1 ten", "Fourteen is one ten and four ones.", "1 ten", "4 tens", "2 tens", "0 tens"),
            math("How many ones are there in 14?", "4 ones", "In fourteen, the ones digit is four.", "4 ones", "1 one", "5 ones", "0 ones"),
            math("How many sides does a triangle have?", "3", "A triangle has three straight sides.", "3", "2", "4", "5"),
            math("Which shape is round and has no corners?", "Circle", "A circle is round and has no corners.", "Circle", "Square", "Triangle", "Rectangle"),
            math("How many sides does a square have?", "4", "A square has four equal sides.", "4", "3", "5", "6")
        );

        Quiz q = new Quiz();
        q.setTitle(title);
        q.setDescription("A beginner-friendly Class 1 Mathematics quiz covering addition, subtraction, number order, comparisons, tens and ones, and basic shapes.");
        q.setCategory("Mathematics");
        q.setTopic("Class 1 Basics");
        q.setHostId(host.getId());
        q.setStatus("DRAFT");
        q.setDurationMinutes(30);
        q.setPointsPerCorrect(1);
        q.setNegativeMarkingEnabled(false);
        q.setPenaltyPerWrong(0);
        q.setAllowRetake(false);
        q.setMaxAttempts(1);
        q.setQuestions(new ArrayList<>(questions));
        q.setUpdatedAt(Instant.now());
        quizzes.save(q);
        System.out.println("Quizora seeded 30 Class 1 Mathematics questions for host1@quiztime.com as a DRAFT quiz.");
    }

    private static Quiz.Question math(String question, String correct, String explanation, String... options) {
        Quiz.Question q = mcq(question, correct, options);
        q.setDifficulty("EASY");
        q.setExplanation(explanation);
        return q;
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
