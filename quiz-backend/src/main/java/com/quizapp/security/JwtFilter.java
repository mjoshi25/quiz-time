package com.quizapp.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.quizapp.repository.UserRepository;

@Component
public class JwtFilter extends OncePerRequestFilter {
	private final JwtService jwt;
	private final UserRepository users;

	public JwtFilter(JwtService j, UserRepository u) {
		jwt = j;
		users = u;
	}

	protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
			throws ServletException, IOException {
		if ("OPTIONS".equalsIgnoreCase(r.getMethod())) {
			c.doFilter(r, s);
			return;
		}
		String h = r.getHeader("Authorization");
		try {
			if (h != null && h.startsWith("Bearer ")) {
				var u = users.findById(jwt.id(h.substring(7))).orElse(null);
				if (u != null && "ACTIVE".equals(u.getAccountStatus())) {
					var a = new UsernamePasswordAuthenticationToken(u.getId(), null,
							List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())));
					SecurityContextHolder.getContext().setAuthentication(a);
				}
			}
		} catch (Exception ignored) {
		}
		c.doFilter(r, s);
	}
}
