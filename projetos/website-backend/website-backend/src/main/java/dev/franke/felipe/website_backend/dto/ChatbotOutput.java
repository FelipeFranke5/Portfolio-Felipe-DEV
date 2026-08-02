package dev.franke.felipe.website_backend.dto;

import java.time.LocalDateTime;

public record ChatbotOutput(String name, String message, LocalDateTime timestamp) {
}
