package com.quizapp.controller;

import com.quizapp.model.QuestionBankItem;
import com.quizapp.repository.QuestionBankRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/host/question-bank")
@PreAuthorize("@hostAuthorization.approved(authentication)")
public class QuestionBankController {
    private final QuestionBankRepository bank;

    public QuestionBankController(QuestionBankRepository bank) { this.bank = bank; }

    @GetMapping
    public List<QuestionBankItem> mine(Authentication auth) {
        return bank.findByHostIdOrderByCreatedAtDesc(auth.getName());
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody QuestionBankItem request, Authentication auth) {
        String error = validate(request);
        if (error != null) return ResponseEntity.badRequest().body(error);
        QuestionBankItem item = new QuestionBankItem();
        copy(request, item);
        item.setHostId(auth.getName());
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(bank.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication auth) {
        QuestionBankItem item = bank.findById(id).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();
        if (!auth.getName().equals(item.getHostId())) return ResponseEntity.status(403).body("You do not own this question.");
        bank.delete(item);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file, Authentication auth) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("Choose a CSV file to import.");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return ResponseEntity.badRequest().body("CSV file is empty.");
            List<String> headers = parseCsvLine(headerLine.replace("\uFEFF", ""));
            String expected = "type,question,optiona,optionb,optionc,optiond,correctanswer,difficulty,explanation,imageurl";
            if (!normalizeHeaders(headers).equals(expected)) {
                return ResponseEntity.badRequest().body("Invalid CSV header. Use: " + expected);
            }
            List<QuestionBankItem> items = new ArrayList<>();
            String line; int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                List<String> c = parseCsvLine(line);
                while (c.size() < 10) c.add("");
                if (c.size() != 10) return ResponseEntity.badRequest().body("Invalid CSV row " + row + ". Expected 10 columns.");
                QuestionBankItem item = new QuestionBankItem();
                item.setType(c.get(0).trim().toUpperCase(Locale.ROOT));
                item.setQuestion(c.get(1).trim());
                item.setOptions(List.of(c.get(2).trim(), c.get(3).trim(), c.get(4).trim(), c.get(5).trim()));
                item.setCorrectAnswer(c.get(6).trim());
                item.setDifficulty(c.get(7).isBlank() ? "MEDIUM" : c.get(7).trim().toUpperCase(Locale.ROOT));
                item.setExplanation(c.get(8).trim());
                item.setImageUrl(c.get(9).trim());
                item.setHostId(auth.getName());
                String error = validate(item);
                if (error != null) return ResponseEntity.badRequest().body("Row " + row + ": " + error);
                items.add(item);
            }
            if (items.isEmpty()) return ResponseEntity.badRequest().body("No question rows were found.");
            Instant now = Instant.now();
            items.forEach(i -> { i.setCreatedAt(now); i.setUpdatedAt(now); });
            bank.saveAll(items);
            return ResponseEntity.ok(java.util.Map.of("imported", items.size(), "message", items.size() + " questions imported successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Unable to import CSV: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(Authentication auth) {
        List<QuestionBankItem> items = bank.findByHostIdOrderByCreatedAtDesc(auth.getName());
        StringBuilder csv = new StringBuilder();
        csv.append("type,question,optionA,optionB,optionC,optionD,correctAnswer,difficulty,explanation,imageUrl\n");
        for (QuestionBankItem i : items) {
            List<String> o = i.getOptions() == null ? List.of() : i.getOptions();
            csv.append(csv(i.getType())).append(',')
               .append(csv(i.getQuestion())).append(',')
               .append(csv(get(o,0))).append(',').append(csv(get(o,1))).append(',')
               .append(csv(get(o,2))).append(',').append(csv(get(o,3))).append(',')
               .append(csv(i.getCorrectAnswer())).append(',').append(csv(i.getDifficulty())).append(',')
               .append(csv(i.getExplanation())).append(',').append(csv(i.getImageUrl())).append('\n');
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("text/csv"));
        h.setContentDisposition(ContentDisposition.attachment().filename("quizora-question-bank.csv").build());
        h.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(h).body(bytes);
    }

    private String validate(QuestionBankItem i) {
        if (i == null || i.getQuestion() == null || i.getQuestion().isBlank()) return "Question text is required.";
        String type = i.getType() == null ? "MCQ" : i.getType().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("MCQ","TRUE_FALSE","CORRECT_WORD").contains(type)) return "Question type must be MCQ, TRUE_FALSE or CORRECT_WORD.";
        if (i.getCorrectAnswer() == null || i.getCorrectAnswer().isBlank()) return "Correct answer is required.";
        String difficulty = i.getDifficulty() == null ? "MEDIUM" : i.getDifficulty().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("EASY","MEDIUM","HARD").contains(difficulty)) return "Difficulty must be EASY, MEDIUM or HARD.";
        if ("TRUE_FALSE".equals(type) && !("true".equalsIgnoreCase(i.getCorrectAnswer().trim()) || "false".equalsIgnoreCase(i.getCorrectAnswer().trim()))) return "True/False answer must be True or False.";
        if (!"TRUE_FALSE".equals(type) && (i.getOptions() == null || i.getOptions().stream().filter(x -> x != null && !x.isBlank()).count() < ("MCQ".equals(type) ? 2 : 1))) return "Add enough answer options.";
        i.setType(type); i.setDifficulty(difficulty);
        if ("TRUE_FALSE".equals(type)) { i.setOptions(new ArrayList<>(List.of("True","False"))); i.setCorrectAnswer("true".equalsIgnoreCase(i.getCorrectAnswer().trim()) ? "True" : "False"); }
        else i.setOptions(i.getOptions() == null ? new ArrayList<>() : new ArrayList<>(i.getOptions()));
        return null;
    }

    private void copy(QuestionBankItem from, QuestionBankItem to) {
        to.setType(from.getType()); to.setQuestion(from.getQuestion().trim());
        to.setOptions(from.getOptions() == null ? new ArrayList<>() : new ArrayList<>(from.getOptions()));
        to.setCorrectAnswer(from.getCorrectAnswer().trim()); to.setDifficulty(from.getDifficulty());
        to.setExplanation(from.getExplanation() == null ? "" : from.getExplanation().trim());
        to.setImageUrl(from.getImageUrl() == null ? "" : from.getImageUrl().trim());
    }
    private static String get(List<String> a, int i){ return i < a.size() && a.get(i) != null ? a.get(i) : ""; }
    private static String csv(String s){ if(s==null)s=""; return "\"" + s.replace("\"", "\"\"") + "\""; }
    private static String normalizeHeaders(List<String> h){ return String.join(",", h).replace(" ", "").toLowerCase(Locale.ROOT); }
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>(); StringBuilder cur = new StringBuilder(); boolean quoted=false;
        for(int i=0;i<line.length();i++){ char ch=line.charAt(i); if(ch=='\"'){ if(quoted && i+1<line.length() && line.charAt(i+1)=='\"'){cur.append('\"');i++;} else quoted=!quoted; } else if(ch==',' && !quoted){out.add(cur.toString());cur.setLength(0);} else cur.append(ch); }
        out.add(cur.toString()); return out;
    }
}
