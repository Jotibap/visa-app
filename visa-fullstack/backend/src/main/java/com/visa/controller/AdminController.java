package com.visa.controller;

import com.visa.entity.*;
import com.visa.repository.*;
import com.visa.service.GeminiVerificationService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final ApplicationRepository apps;
  private final UserRepository users;
  private final GeminiVerificationService ai;

  public AdminController(ApplicationRepository a, UserRepository u, GeminiVerificationService g) {
    apps = a;
    users = u;
    ai = g;
  }

  @GetMapping("/dashboard")
  public Object dashboard() {
    List<VisaApplication> all = apps.findAll();
    return Map.of("totalApplications", all.size(), "pendingReview",
        all.stream()
            .filter(x -> List.of("Submitted", "In Progress", "In Review", "Requires Attention").contains(x.getStatus()))
            .count(),
        "approvedToday",
        all.stream()
            .filter(x -> x.getStatus().equals("Approved") && x.getUpdatedAt().atZone(java.time.ZoneOffset.UTC)
                .toLocalDate().equals(java.time.LocalDate.now(java.time.ZoneOffset.UTC)))
            .count(),
        "rejectedToday",
        all.stream()
            .filter(x -> x.getStatus().equals("Rejected") && x.getUpdatedAt().atZone(java.time.ZoneOffset.UTC)
                .toLocalDate().equals(java.time.LocalDate.now(java.time.ZoneOffset.UTC)))
            .count(),
        "totalUsers", users.count());
  }

  @GetMapping("/applications")
  public Object list(@RequestParam(required = false) String status, @RequestParam(required = false) String search,
      @RequestParam(required = false) String risk, @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo, @RequestParam(defaultValue = "updatedAt") String sort) {
    return apps.findAll().stream()
        .filter(a -> status == null || status.isBlank() || a.getStatus().equalsIgnoreCase(status))
        .filter(a -> risk == null || risk.isBlank() || risk.equalsIgnoreCase("All")
            || Objects.equals(a.getAiRiskScore(), risk))
        .filter(a -> search == null || search.isBlank() || String.valueOf(a.getId()).contains(search)
            || a.getUser().getName().toLowerCase().contains(search.toLowerCase())
            || a.getUser().getEmail().toLowerCase().contains(search.toLowerCase()))
        .filter(a -> dateFrom == null || dateFrom.isBlank()
            || (a.getSubmittedAt() != null && a.getSubmittedAt().toString().substring(0, 10).compareTo(dateFrom) >= 0))
        .filter(a -> dateTo == null || dateTo.isBlank()
            || (a.getSubmittedAt() != null && a.getSubmittedAt().toString().substring(0, 10).compareTo(dateTo) <= 0))
        .sorted(comparator(sort)).map(this::view).toList();
  }

  @GetMapping("/applications/{id}")
  public Object detail(@PathVariable Long id) {
    return view(apps.findById(id).orElseThrow());
  }

  @PatchMapping("/applications/{id}")
  public Object update(@PathVariable Long id, @RequestBody Map<String, String> body) {
    VisaApplication a = apps.findById(id).orElseThrow();
    String status = body.get("status");
    if (status != null
        && !List.of("Draft", "Submitted", "In Progress", "In Review", "Requires Attention", "Approved", "Rejected")
            .contains(status))
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
    if (status != null)
      a.setStatus(status);
    if (("Rejected".equals(status) || "Requires Attention".equals(status))
        && (body.get("applicantFeedback") == null || body.get("applicantFeedback").isBlank()))
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Applicant feedback is required for this status");
    if (body.containsKey("applicantFeedback"))
      a.setApplicantFeedback(body.get("applicantFeedback"));
    if (body.containsKey("adminNotes"))
      a.setAdminNotes(body.get("adminNotes"));
    return view(apps.save(a));
  }

  @PostMapping("/applications/{id}/trigger-ai")
  public Object trigger(@PathVariable Long id) {
    VisaApplication a = apps.findById(id).orElseThrow();
    try {
      var result = ai.analyze(a);
      a.setAiFindings((Map<String, Object>) result.get("findings"));
      a.setAiRiskScore((String) result.get("riskScore"));
      a.setAiSummary((String) result.get("summary"));
      if (a.getStatus().equals("Submitted"))
        a.setStatus("In Review");
      apps.save(a);
      return view(a);
    } catch (Exception e) {
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "AI verification failed: " + e.getMessage());
    }
  }

  private Comparator<VisaApplication> comparator(String sort) {
    return switch (sort.toLowerCase()) {
      case "applicant" -> Comparator.comparing((VisaApplication a) -> a.getUser().getName().toLowerCase());
      case "status" -> Comparator.comparing(VisaApplication::getStatus);
      case "submitted" -> Comparator
          .comparing(VisaApplication::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
      default -> Comparator.comparing(VisaApplication::getUpdatedAt).reversed();
    };
  }

  private Map<String, Object> view(VisaApplication a) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", a.getId());
    m.put("applicant",
        Map.of("id", a.getUser().getId(), "name", a.getUser().getName(), "email", a.getUser().getEmail()));
    m.put("status", a.getStatus());
    m.put("aiRiskScore", a.getAiRiskScore());
    m.put("aiSummary", a.getAiSummary());
    m.put("aiFindings", a.getAiFindings());
    m.put("submittedAt", a.getSubmittedAt());
    m.put("updatedAt", a.getUpdatedAt());
    m.put("visaDetails", a.getVisaDetails());
    m.put("personalDetails", a.getPersonalDetails());
    m.put("passportDetails", a.getPassportDetails());
    m.put("addressDetails", a.getAddressDetails());
    m.put("securityDeclaration", a.getSecurityDeclaration());
    m.put("adminNotes", a.getAdminNotes());
    m.put("applicantFeedback", a.getApplicantFeedback());
    return m;
  }
}
