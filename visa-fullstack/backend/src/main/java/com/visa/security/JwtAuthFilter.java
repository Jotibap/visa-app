package com.visa.security;

import com.visa.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService j, UserRepository u) {
        jwt = j;
        users = u;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer "))
            try {
                String email = jwt.subject(h.substring(7));
                users.findByEmail(email).ifPresent(u -> {
                    var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + u.getRole().toUpperCase()));
                    if (u.getEmailVerifiedAt() != null)
                        authorities.add(new SimpleGrantedAuthority("ROLE_EMAIL_VERIFIED"));
                    var a = new UsernamePasswordAuthenticationToken(u.getEmail(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(a);
                });
            } catch (Exception ignored) {
            }
        chain.doFilter(req, res);
    }
}
