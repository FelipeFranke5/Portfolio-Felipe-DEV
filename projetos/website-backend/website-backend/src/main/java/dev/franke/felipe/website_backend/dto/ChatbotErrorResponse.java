package dev.franke.felipe.website_backend.dto;

import java.time.Instant;

public record ChatbotErrorResponse(Instant currentTime, String message) {
}
