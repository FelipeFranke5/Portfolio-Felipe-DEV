package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.dto.ChatbotInput;
import dev.franke.felipe.website_backend.exception.ChatbotRateLimitException;
import dev.franke.felipe.website_backend.service.ChatbotInputSanitizer;
import dev.franke.felipe.website_backend.service.ChatbotRateLimiter;
import dev.franke.felipe.website_backend.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatbotController {

    private static final String DEFAULT_DISPLAY_NAME = "você";

    private final ChatbotService chatbotService;
    private final ChatbotInputSanitizer chatbotInputSanitizer;
    private final ChatbotRateLimiter chatbotRateLimiter;

    @MessageMapping("/new-message")
    public void newMessage(@Valid @Payload ChatbotInput input, Principal principal) {
        String user = principal.getName();

        /*
            A cota é checada antes de qualquer trabalho: nenhuma chamada paga acontece
            depois que o usuário estoura o limite.
         */
        ChatbotRateLimiter.AllowUserDecision decision = chatbotRateLimiter.tryConsume(user);

        if (!decision.isAllowed()) {
            throw new ChatbotRateLimitException(decision.decisionMessage());
        }

        String userQuestion = chatbotInputSanitizer.sanitize(input.message());
        chatbotService.sendMessages(
            user, 
            retrieveDisplayNameFromPrincipal(principal), 
            userQuestion
        );
    }

    /**
     * O nome amigável sai do próprio token, não de {@code @AuthenticationPrincipal}: aquele
     * resolver só é registrado por {@code @EnableWebSocketSecurity}, que não usamos aqui
     * (ver a nota no {@code JwtStompChannelInterceptor}). Sem o resolver, o método sequer
     * seria invocado.
     */
    private String retrieveDisplayNameFromPrincipal(Principal principal) {
        if (!(principal instanceof JwtAuthenticationToken auth)) {
            return DEFAULT_DISPLAY_NAME;
        }

        String displayName = auth.getToken().getClaimAsString("preferred_username");
        return displayName != null && !displayName.isBlank() ? displayName : DEFAULT_DISPLAY_NAME;
    }
}
