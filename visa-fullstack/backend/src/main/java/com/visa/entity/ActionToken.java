package com.visa.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "action_tokens", indexes = @Index(name = "idx_action_token_lookup", columnList = "token,purpose,consumed"))
public class ActionToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;
    @Column(nullable = false, unique = true, length = 128)
    private String token;
    @Column(nullable = false, length = 32)
    private String purpose;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean consumed = false;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ActionToken() {
    }

    public ActionToken(AppUser user, String token, String purpose, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public String getPurpose() {
        return purpose;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void consume() {
        consumed = true;
    }
}
