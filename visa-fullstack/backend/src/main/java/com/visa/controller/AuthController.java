package com.visa.controller;

import com.visa.entity.ActionToken;
import com.visa.entity.AppUser;
import com.visa.repository.ActionTokenRepository;
import com.visa.repository.UserRepository;
import com.visa.security.JwtService;
import com.visa.service.EmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final ActionTokenRepository tokens;
    private final EmailService email;

    public AuthController(UserRepository u, PasswordEncoder e, JwtService j, ActionTokenRepository t,
            EmailService mail) {
        users = u;
        encoder = e;
        jwt = j;
        tokens = t;
        email = mail;
    }

    public record Register(@NotBlank @Size(max = 120) String name, @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record Login(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record ForgotPassword(@Email @NotBlank String email) {
    }

    public record ResetPassword(@NotBlank String token, @NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record UpdateProfile(@NotBlank @Size(max = 120) String name, @Email @NotBlank String email) {
    }

    public record ChangePassword(@NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100) String newPassword) {
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Register r) {
        String address = r.email().trim().toLowerCase();
        if (users.existsByEmail(address))
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        AppUser u = new AppUser();
        u.setName(r.name().trim());
        u.setEmail(address);
        u.setPassword(encoder.encode(r.password()));
        u.setRole("user");
        users.save(u);
        if (email.isEnabled())
            issueToken(u, "VERIFY_EMAIL", DurationDays.ONE);
        else {
            u.setEmailVerifiedAt(Instant.now());
            users.save(u);
        }
        return ResponseEntity.status(201).body(auth(u));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login r) {
        var u = users.findByEmail(r.email().trim().toLowerCase()).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPassword()))
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        return ResponseEntity.ok(auth(u));
    }

    @GetMapping("/me")
    public Object me(Authentication a) {
        return user(current(a));
    }

    @PostMapping("/forgot-password")
    public Object forgot(@Valid @RequestBody ForgotPassword r) {
        users.findByEmail(r.email().trim().toLowerCase())
                .ifPresent(u -> issueToken(u, "RESET_PASSWORD", DurationDays.THIRTY_MINUTES));
        return Map.of("message", "If the email is registered, password reset instructions have been sent.");
    }

    @PostMapping("/reset-password")
    @Transactional
    public Object reset(@Valid @RequestBody ResetPassword r) {
        ActionToken t = tokens.findByTokenAndPurposeAndConsumedFalse(r.token(), "RESET_PASSWORD").orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));
        if (t.getExpiresAt().isBefore(Instant.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link");
        t.getUser().setPassword(encoder.encode(r.password()));
        t.consume();
        users.save(t.getUser());
        return Map.of("message", "Password has been reset successfully. Please sign in.");
    }

    @PostMapping("/verify-email")
    @Transactional
    public Object verify(@RequestParam String token) {
        ActionToken t = tokens.findByTokenAndPurposeAndConsumedFalse(token, "VERIFY_EMAIL").orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification link"));
        if (t.getExpiresAt().isBefore(Instant.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification link");
        t.getUser().setEmailVerifiedAt(Instant.now());
        t.consume();
        users.save(t.getUser());
        return Map.of("message", "Email verified successfully.");
    }

    @PostMapping("/resend-verification")
    public Object resend(Authentication a) {
        AppUser u = current(a);
        if (u.getEmailVerifiedAt() == null)
            issueToken(u, "VERIFY_EMAIL", DurationDays.ONE);
        return Map.of("message", "If verification is required, a new email has been sent.");
    }

    @PatchMapping("/profile")
    public Object profile(@Valid @RequestBody UpdateProfile r, Authentication a) {
        AppUser u = current(a);
        String emailAddress = r.email().trim().toLowerCase();
        if (!u.getEmail().equals(emailAddress) && users.existsByEmail(emailAddress))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address is already in use");
        boolean changed = !u.getEmail().equals(emailAddress);
        u.setName(r.name().trim());
        u.setEmail(emailAddress);
        if (changed) {
            u.setEmailVerifiedAt(null);
            if (email.isEnabled())
                issueToken(u, "VERIFY_EMAIL", DurationDays.ONE);
            else
                u.setEmailVerifiedAt(Instant.now());
        }
        users.save(u);
        return Map.of("token", jwt.generate(u.getEmail()), "user", user(u));
    }

    @PatchMapping("/password")
    public Object password(@Valid @RequestBody ChangePassword r, Authentication a) {
        AppUser u = current(a);
        if (!encoder.matches(r.currentPassword(), u.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        u.setPassword(encoder.encode(r.newPassword()));
        users.save(u);
        return Map.of("message", "Password updated successfully.");
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> delete(@RequestBody Map<String, String> body, Authentication a) {
        AppUser u = current(a);
        if (!encoder.matches(body.getOrDefault("password", ""), u.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is incorrect");
        tokens.deleteByUserIdAndPurpose(u.getId(), "VERIFY_EMAIL");
        tokens.deleteByUserIdAndPurpose(u.getId(), "RESET_PASSWORD");
        users.delete(u);
        return ResponseEntity.noContent().build();
    }

    private AppUser current(Authentication a) {
        return users.findByEmail(a.getName()).orElseThrow();
    }

    private void issueToken(AppUser u, String purpose, DurationDays duration) {
        tokens.deleteByUserIdAndPurpose(u.getId(), purpose);
        String value = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        ActionToken t = new ActionToken(u, value, purpose, Instant.now().plusSeconds(duration.seconds));
        tokens.save(t);
        if (purpose.equals("VERIFY_EMAIL"))
            email.sendVerification(u.getEmail(), u.getName(), value);
        else
            email.sendPasswordReset(u.getEmail(), u.getName(), value);
    }

    private Map<String, Object> auth(AppUser u) {
        return Map.of("token", jwt.generate(u.getEmail()), "user", user(u));
    }

    private Map<String, Object> user(AppUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("emailVerified", u.getEmailVerifiedAt() != null);
        return m;
    }

    private enum DurationDays {
        ONE(86400), THIRTY_MINUTES(1800);

        final long seconds;

        DurationDays(long s) {
            seconds = s;
        }
    }
}
