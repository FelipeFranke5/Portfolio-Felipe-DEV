package dev.franke.felipe.website_backend.exception;

/**
 * Sinaliza que o usuário estourou a cota de uso do chatbot. Diferente das demais exceções
 * do chat, não indica erro de programação nem payload inválido — é o limite de custo
 * funcionando.
 */
public class ChatbotRateLimitException extends RuntimeException {

    public ChatbotRateLimitException(String message) {
        super(message);
    }
}
