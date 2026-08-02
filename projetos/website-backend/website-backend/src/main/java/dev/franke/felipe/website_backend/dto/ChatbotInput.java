package dev.franke.felipe.website_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotInput(
        @NotBlank
        @Size(max = 100)
        String name,
        @NotBlank
        @Size(max = 100)
        String message
) {
}
