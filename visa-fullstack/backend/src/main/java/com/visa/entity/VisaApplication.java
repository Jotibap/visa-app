package com.visa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "applications")
public class VisaApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;
    @Column(nullable = false)
    private String status = "Draft";
    private String aiRiskScore;
    @Column(columnDefinition = "text")
    private String aiSummary;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> aiFindings = new HashMap<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> visaDetails = new HashMap<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> personalDetails = new HashMap<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> passportDetails = new HashMap<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> addressDetails = new HashMap<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> securityDeclaration = new HashMap<>();
    private Instant submittedAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    @Column(columnDefinition = "text")
    private String adminNotes;
    @Column(columnDefinition = "text")
    private String applicantFeedback;

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser v) {
        user = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
        touch();
    }

    public String getAiRiskScore() {
        return aiRiskScore;
    }

    public void setAiRiskScore(String v) {
        aiRiskScore = v;
        touch();
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String v) {
        aiSummary = v;
        touch();
    }

    public Map<String, Object> getAiFindings() {
        return aiFindings;
    }

    public void setAiFindings(Map<String, Object> v) {
        aiFindings = v;
        touch();
    }

    public Map<String, Object> getVisaDetails() {
        return visaDetails;
    }

    public Map<String, Object> getPersonalDetails() {
        return personalDetails;
    }

    public Map<String, Object> getPassportDetails() {
        return passportDetails;
    }

    public Map<String, Object> getAddressDetails() {
        return addressDetails;
    }

    public Map<String, Object> getSecurityDeclaration() {
        return securityDeclaration;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant v) {
        submittedAt = v;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String v) {
        adminNotes = v;
        touch();
    }

    public String getApplicantFeedback() {
        return applicantFeedback;
    }

    public void setApplicantFeedback(String v) {
        applicantFeedback = v;
        touch();
    }

    public void updateSection(String section, Map<String, Object> data) {
        switch (section) {
            case "visa-details" -> visaDetails = data;
            case "personal-details" -> personalDetails = data;
            case "passport-details" -> passportDetails = data;
            case "address-details" -> addressDetails = data;
            case "security-declaration" -> securityDeclaration = data;
            default -> throw new IllegalArgumentException("Unknown application section");
        }
        touch();
    }

    public void touch() {
        updatedAt = Instant.now();
    }
}
