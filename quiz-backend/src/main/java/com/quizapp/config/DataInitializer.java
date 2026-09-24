package com.quizapp.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.quizapp.repository.UserRepository;
import com.quizapp.model.User;

@Configuration
public class DataInitializer {
	@Bean
	CommandLineRunner admin(UserRepository r, PasswordEncoder p, @Value("${app.admin.email}") String e,
			@Value("${app.admin.password}") String pw) {
		return a -> {
			if (r.findByEmailIgnoreCase(e).isEmpty()) {
				User u = new User();
				u.setName("System Admin");
				u.setEmail(e.toLowerCase());
				u.setPasswordHash(p.encode(pw));
				u.setRole("ADMIN");
				u.setAccountStatus("ACTIVE");
				u.setHostApprovalStatus("NOT_APPLICABLE");
				r.save(u);
			}
		};
	}
}
