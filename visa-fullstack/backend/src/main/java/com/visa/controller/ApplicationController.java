package com.visa.controller;

import com.visa.entity.*;
import com.visa.repository.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationRepository apps;
    private final UserRepository users;
    @Value("${app.upload-dir}")
    private String uploadDir;

    public ApplicationController(ApplicationRepository a, UserRepository u) {
        apps = a;
        users = u;
    }

    private AppUser current(Authentication a) {
        return users.findByEmail(a.getName()).orElseThrow();
    }

    private void own(VisaApplication app, AppUser u) {
        if (!app.getUser().getId().equals(u.getId()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @GetMapping
    public List<?> mine(Authentication a) {
        return apps.findByUserIdOrderByUpdatedAtDesc(current(a).getId()).stream().map(this::view).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(Authentication a) {
        AppUser u = current(a);
        boolean active = apps.findByUserIdOrderByUpdatedAtDesc(u.getId()).stream()
                .anyMatch(x -> !List.of("Draft", "Rejected").contains(x.getStatus()));
        if (active)
            return ResponseEntity.badRequest().body(Map.of("message", "You already have an active application."));
        VisaApplication v = new VisaApplication();
        v.setUser(u);
        return ResponseEntity.status(201).body(view(apps.save(v)));
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable Long id, Authentication a) {
        var v = apps.findById(id).orElseThrow();
        own(v, current(a));
        return view(v);
    }

    @PatchMapping("/{id}/sections/{section}")
    public Object section(@PathVariable Long id, @PathVariable String section, @RequestBody Map<String, Object> body,
            Authentication a) {
        var v = apps.findById(id).orElseThrow();
        own(v, current(a));
        if (!v.getStatus().equals("Draft"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,
                    "Submitted applications cannot be edited");
        validateSection(section, body, id);
        v.updateSection(section, body);
        return view(apps.save(v));
    }

    @PostMapping("/{id}/submit")
    public Object submit(@PathVariable Long id, Authentication a) {
        var v = apps.findById(id).orElseThrow();
        own(v, current(a));
        if (!v.getStatus().equals("Draft"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,
                    "Application has already been submitted");
        require(v.getVisaDetails(), "visa_category", "visa_type", "embassy");
        require(v.getPersonalDetails(), "photo_path", "pan_photo", "adhar_photo", "first_name", "last_name", "gender",
                "date_of_birth", "nationality", "country_of_birth", "marital_status", "occupation", "email", "mobile");
        require(v.getPassportDetails(), "passport_number", "passport_issued_date", "passport_expiry_date",
                "passport_issued_by", "passport_issued_country", "passport_image_path");
        require(v.getAddressDetails(), "street_address", "city", "state_province", "country", "postal_code");
        Map<String, Object> sec = v.getSecurityDeclaration();
        require(sec, "crime_conviction", "visa_refusal");
        validateSubmittedFields(v);
        if (!Boolean.TRUE.equals(sec.get("declaration_agreed")))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You must accept the declaration before submitting");
        if (affirmative(sec.get("crime_conviction")) && blank(sec.get("crime_details")))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Crime details are required");
        if (affirmative(sec.get("visa_refusal")) && blank(sec.get("visa_refusal_details")))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Visa refusal details are required");
        v.setStatus("Submitted");
        v.setSubmittedAt(java.time.Instant.now());
        return view(apps.save(v));
    }

    private void validateSubmittedFields(VisaApplication v) {
        Map<String, Object> p = v.getPersonalDetails(), passport = v.getPassportDetails(),
                sec = v.getSecurityDeclaration();
        for (String key : List.of("first_name", "last_name")) {
            String value = String.valueOf(p.get(key));
            if (!value.matches("[\\p{L}]+"))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        key + " must contain letters only");
        }
        if (!List.of("male", "female", "other").contains(String.valueOf(p.get("gender")).toLowerCase()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid gender");
        if (!List.of("single", "married", "divorced", "widowed")
                .contains(String.valueOf(p.get("marital_status")).toLowerCase()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid marital status");
        try {
            java.time.LocalDate dob = java.time.LocalDate.parse(String.valueOf(p.get("date_of_birth")));
            if (dob.isAfter(java.time.LocalDate.now()))
                throw new IllegalArgumentException();
            java.time.LocalDate issued = java.time.LocalDate
                    .parse(String.valueOf(passport.get("passport_issued_date")));
            java.time.LocalDate expiry = java.time.LocalDate
                    .parse(String.valueOf(passport.get("passport_expiry_date")));
            if (issued.isAfter(java.time.LocalDate.now()) || !expiry.isAfter(issued))
                throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Check date of birth and passport issue/expiry dates");
        }
        if (!String.valueOf(p.get("email")).matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a valid email address");
        if (!String.valueOf(p.get("mobile")).matches("^[0-9 +()\\-]{10,20}$"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a valid mobile number");
        if (!String.valueOf(passport.get("passport_number")).matches("[A-Za-z0-9]+"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Passport number must be alphanumeric");
        if (affirmative(sec.get("crime_conviction")) && blank(sec.get("crime_details")))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Crime details are required");
        if (affirmative(sec.get("visa_refusal")) && blank(sec.get("visa_refusal_details")))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Visa refusal details are required");
    }

    private boolean affirmative(Object value) {
        return Boolean.TRUE.equals(value) || "yes".equalsIgnoreCase(String.valueOf(value));
    }

    private boolean blank(Object value) {
        return value == null || value.toString().isBlank();
    }

    private void require(Map<String, Object> data, String... keys) {
        for (String key : keys)
            if (blank(data.get(key)))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Please complete the required field: " + key);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication a) {
        var v = apps.findById(id).orElseThrow();
        own(v, current(a));
        if (!v.getStatus().equals("Draft"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,
                    "Only draft applications can be deleted");
        apps.delete(v);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/files")
    public Object upload(@PathVariable Long id, @RequestParam("file") MultipartFile file, @RequestParam String field,
            Authentication a) throws Exception {
        var v = apps.findById(id).orElseThrow();
        own(v, current(a));
        if (!"Draft".equals(v.getStatus()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,
                    "Submitted applications cannot be changed");
        if (file.isEmpty() || file.getSize() > ("passport_image_path".equals(field) ? 2 : 12) * 1024 * 1024)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File is empty or exceeds the allowed size (2 MB for passport, 12 MB for other images)");
        String type = file.getContentType();
        if (!List.of("photo_path", "pan_photo", "adhar_photo", "passport_image_path").contains(field))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown upload field");
        if (type == null || !(List.of("image/jpeg", "image/png", "image/webp").contains(type)
                || ("passport_image_path".equals(field) && "application/pdf".equals(type))))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type");
        Path dir = Paths.get(uploadDir, "application-" + id);
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + "-"
                + Path.of(Objects.requireNonNull(file.getOriginalFilename())).getFileName();
        Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return Map.of("field", field, "path", "/api/applications/" + id + "/files/" + name, "originalName",
                file.getOriginalFilename());
    }

    @GetMapping("/{id}/files/{filename}")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable String filename, Authentication a)
            throws Exception {
        var v = apps.findById(id).orElseThrow();
        AppUser requester = current(a);
        if (!v.getUser().getId().equals(requester.getId()) && !"admin".equalsIgnoreCase(requester.getRole()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        String safe = Path.of(filename).getFileName().toString();
        Path path = Paths.get(uploadDir, "application-" + id, safe).normalize();
        if (!path.startsWith(Paths.get(uploadDir).resolve("application-" + id).normalize()) || !Files.exists(path))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
        Resource resource = new UrlResource(path.toUri());
        String mime = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(mime == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safe + "\"").body(resource);
    }

    private void validateSection(String section, Map<String, Object> body, Long applicationId) {
        Map<String, Set<String>> allowed = Map.of(
                "visa-details", Set.of("visa_type", "visa_category", "embassy"),
                "personal-details",
                Set.of("photo_path", "pan_photo", "adhar_photo", "first_name", "last_name", "gender", "date_of_birth",
                        "nationality", "country_of_birth", "marital_status", "occupation", "email", "mobile"),
                "passport-details",
                Set.of("passport_number", "passport_expiry_date", "passport_issued_date", "passport_issued_by",
                        "passport_issued_country", "passport_image_path"),
                "address-details",
                Set.of("street_address", "apartment_unit_no", "city", "state_province", "country", "postal_code"),
                "security-declaration", Set.of("crime_conviction", "crime_details", "visa_refusal",
                        "visa_refusal_details", "declaration_agreed"));
        Set<String> keys = allowed.get(section);
        if (keys == null)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown application section");
        Map<String, List<String>> required = Map.of(
                "visa-details", List.of("visa_type", "visa_category", "embassy"),
                "personal-details",
                List.of("photo_path", "pan_photo", "adhar_photo", "first_name", "last_name", "gender", "date_of_birth",
                        "nationality", "country_of_birth", "marital_status", "occupation", "email", "mobile"),
                "passport-details",
                List.of("passport_number", "passport_issued_date", "passport_expiry_date", "passport_issued_by",
                        "passport_issued_country", "passport_image_path"),
                "address-details", List.of("street_address", "city", "state_province", "country", "postal_code"),
                "security-declaration", List.of("crime_conviction", "visa_refusal", "declaration_agreed"));
        for (String field : required.get(section))
            if (!body.containsKey(field) || body.get(field) == null || body.get(field).toString().isBlank())
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        field.replace('_', ' ') + " is required");
        if ("security-declaration".equals(section)) {
            if (!Boolean.TRUE.equals(body.get("declaration_agreed")))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You must accept the declaration");
            if (affirmative(body.get("crime_conviction"))
                    && (body.get("crime_details") == null || body.get("crime_details").toString().isBlank()))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Crime details are required");
            if (affirmative(body.get("visa_refusal")) && (body.get("visa_refusal_details") == null
                    || body.get("visa_refusal_details").toString().isBlank()))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Visa refusal details are required");
        }
        for (var e : body.entrySet()) {
            if (!keys.contains(e.getKey()))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown field: " + e.getKey());
            Object value = e.getValue();
            if (value instanceof String text && text.length() > 4000)
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        e.getKey() + " is too long");
        }
        for (String key : List.of("photo_path", "pan_photo", "adhar_photo", "passport_image_path")) {
            Object value = body.get(key);
            if (value instanceof String path && !path.isBlank()
                    && !path.startsWith("/api/applications/" + applicationId + "/files/"))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid uploaded file reference");
        }
    }

    public Map<String, Object> view(VisaApplication v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("status", v.getStatus());
        m.put("aiRiskScore", v.getAiRiskScore());
        m.put("aiSummary", v.getAiSummary());
        m.put("aiFindings", v.getAiFindings());
        m.put("submittedAt", v.getSubmittedAt());
        m.put("createdAt", v.getCreatedAt());
        m.put("updatedAt", v.getUpdatedAt());
        m.put("visaDetails", v.getVisaDetails());
        m.put("personalDetails", v.getPersonalDetails());
        m.put("passportDetails", v.getPassportDetails());
        m.put("addressDetails", v.getAddressDetails());
        m.put("securityDeclaration", v.getSecurityDeclaration());
        m.put("applicantFeedback", v.getApplicantFeedback());
        return m;
    }
}
