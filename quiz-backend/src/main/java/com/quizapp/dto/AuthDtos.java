package com.quizapp.dto;
public class AuthDtos {
    public record RegisterRequest(String name,String email,String password){}
    public record LoginRequest(String email,String password){}
    public record AuthResponse(String token,String id,String name,String email,String role,String hostApprovalStatus,String accountStatus,String profilePhoto){}
}
