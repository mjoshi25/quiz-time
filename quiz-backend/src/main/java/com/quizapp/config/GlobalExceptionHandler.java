package com.quizapp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<Map<String,Object>> body(HttpStatus status, String message, HttpServletRequest req){
        Map<String,Object> m=new LinkedHashMap<>(); m.put("timestamp", Instant.now().toString());
        m.put("status", status.value()); m.put("error", status.getReasonPhrase());
        m.put("message", message); m.put("path", req.getRequestURI());
        return ResponseEntity.status(status).body(m);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e,HttpServletRequest r){
        String msg=e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+": "+x.getDefaultMessage()).orElse("Validation failed");
        return body(HttpStatus.BAD_REQUEST,msg,r);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String,Object>> illegal(IllegalArgumentException e,HttpServletRequest r){return body(HttpStatus.BAD_REQUEST,e.getMessage()==null?"Invalid request":e.getMessage(),r);}
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,Object>> generic(Exception e,HttpServletRequest r){return body(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected server error occurred.",r);}
}
