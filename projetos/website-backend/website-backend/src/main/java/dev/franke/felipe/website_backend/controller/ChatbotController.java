package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.dto.ChatbotInput;
import dev.franke.felipe.website_backend.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @MessageMapping("/new-message")
    public void newMessage(@Valid @Payload ChatbotInput input) {
        String validatedMessage = chatbotService.validateAndReturnMessage(input.message(), "mensagem");
        String validatedName = chatbotService.validateAndReturnMessage(input.name(), "nome");
        ChatbotInput validatedInput = new ChatbotInput(validatedName, validatedMessage);
        chatbotService.sendMessages(validatedInput);
    }
}
