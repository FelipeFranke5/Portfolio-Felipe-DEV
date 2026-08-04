package dev.franke.felipe.website_backend.dto;

import java.time.Instant;

public record ChatbotOutput(String name, String message, Instant timestamp) {
}
