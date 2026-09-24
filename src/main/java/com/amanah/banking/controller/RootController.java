package com.amanah.banking.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> status() {
        return Map.of(
            "service", "AMANAH Banking API",
            "status", "running",
            "login", "POST /api/auth/login"
        );
    }
}
