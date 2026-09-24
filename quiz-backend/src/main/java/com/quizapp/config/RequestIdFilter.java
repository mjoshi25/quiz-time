package com.quizapp.config;

import jakarta.servlet.*;import jakarta.servlet.http.*;import org.springframework.core.Ordered;import org.springframework.web.filter.OncePerRequestFilter;import org.springframework.core.annotation.Order;import org.springframework.stereotype.Component;import java.io.IOException;import java.util.UUID;

@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        String id=req.getHeader("X-Request-Id"); if(id==null||id.isBlank()||id.length()>100) id=UUID.randomUUID().toString();
        res.setHeader("X-Request-Id",id); chain.doFilter(req,res);
    }
}
