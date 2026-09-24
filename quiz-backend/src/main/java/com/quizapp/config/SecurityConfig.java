package com.quizapp.config;

import com.quizapp.security.JwtFilter;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean SecurityFilterChain filter(HttpSecurity h, JwtFilter f)throws Exception{
        return h.csrf(x->x.disable()).cors(x->{}).headers(headers -> headers
                .contentTypeOptions(x -> {})
                .frameOptions(x -> x.deny())
                .referrerPolicy(x -> x.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .httpStrictTransportSecurity(x -> x.includeSubDomains(true).maxAgeInSeconds(31536000)))
            .sessionManagement(x->x.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(x->x
                .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                .requestMatchers("/actuator/health","/actuator/info","/api/auth/**","/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/host/**").hasRole("HOST")
                .requestMatchers("/api/participant/**").hasRole("PARTICIPANT")
                .requestMatchers("/api/notifications/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class).build();
    }
}