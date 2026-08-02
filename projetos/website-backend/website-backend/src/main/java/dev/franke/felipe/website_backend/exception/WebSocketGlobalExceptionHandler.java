package dev.franke.felipe.website_backend.exception;

import dev.franke.felipe.website_backend.dto.ChatbotErrorResponse;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.LocalDateTime;

@ControllerAdvice
public class WebSocketGlobalExceptionHandler {

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ChatbotErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return new ChatbotErrorResponse(
                LocalDateTime.now(),
                "Mensagem Inválida!"
        );
    }
}
