package dev.franke.felipe.website_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload que o cliente envia em {@code /chatbot/new-message}.
 *
 * <p>Não há campo de nome: a identidade do remetente vem do JWT da sessão STOMP. Aceitar
 * o nome pelo payload deixava o cliente escolher para qual "usuário" a resposta seria
 * roteada.
 */
public record ChatbotInput(

        @NotBlank(message = "A mensagem é obrigatória")
        @Size(max = 500, message = "A mensagem não pode ter mais de 500 caracteres")
        String message
) {
}
