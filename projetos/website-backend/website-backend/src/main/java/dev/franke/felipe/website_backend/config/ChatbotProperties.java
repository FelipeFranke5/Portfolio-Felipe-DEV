package dev.franke.felipe.website_backend.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Limites de uso do chatbot. A IA é paga por token, então cada pergunta tem custo real —
 * estes valores são o teto de gasto por usuário.
 */
@ConfigurationProperties(prefix = "chatbot.quota")
@Validated
public record ChatbotProperties(

        /** Janela deslizante de 1 minuto, por usuário autenticado. Barra flood. */
        @DefaultValue("5") @Min(1) int maxPorMinuto,

        /** Teto diário por usuário autenticado. */
        @DefaultValue("40") @Min(1) int maxPorUsuarioPorDia,

        /** Concorrência máxima de chamadas à IA no processo inteiro. */
        @DefaultValue("2") @Min(1) int maxChamadasSimultaneas
) {
}
