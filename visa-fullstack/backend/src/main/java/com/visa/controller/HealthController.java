package com.visa.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Object health() {
        return Map.of("status", "ok", "service", "visa-backend");
    }
}
