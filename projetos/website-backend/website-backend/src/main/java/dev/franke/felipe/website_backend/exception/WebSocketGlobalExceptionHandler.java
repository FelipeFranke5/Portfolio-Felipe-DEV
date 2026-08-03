package dev.franke.felipe.website_backend.exception;

import dev.franke.felipe.website_backend.dto.ChatbotErrorResponse;
import dev.franke.felipe.website_backend.model.InternalLog;
import dev.franke.felipe.website_backend.service.ChatbotService;
import dev.franke.felipe.website_backend.service.InternalLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
/*
    ATENÇÃO: é a MethodArgumentNotValidException do *messaging*, não a de
    org.springframework.web.bind. São classes distintas e sem relação de herança — a versão
    anterior importava a do web, então o handler nunca era acionado e as falhas de validação
    morriam em silêncio.
 */
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Traduz as falhas do fluxo STOMP em mensagens na fila de erro do próprio usuário.
 *
 * <p>{@code broadcast = false} é obrigatório aqui: com o padrão {@code true}, o erro de uma
 * aba iria para todas as sessões abertas daquele usuário.
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class WebSocketGlobalExceptionHandler {

    private final InternalLogService internalLogService;

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = ChatbotService.ERRORS_QUEUE_PATH, broadcast = false)
    public ChatbotErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        log.info("Received invalid payload from chatbot");
        return new ChatbotErrorResponse(LocalDateTime.now(), "Mensagem inválida!");
    }

    @MessageExceptionHandler(ChatbotRateLimitException.class)
    @SendToUser(destinations = ChatbotService.ERRORS_QUEUE_PATH, broadcast = false)
    public ChatbotErrorResponse handleChatbotRateLimitException(ChatbotRateLimitException exception) {
        return new ChatbotErrorResponse(LocalDateTime.now(), exception.getMessage());
    }

    @MessageExceptionHandler(ChatbotGeneralException.class)
    @SendToUser(destinations = ChatbotService.ERRORS_QUEUE_PATH, broadcast = false)
    public ChatbotErrorResponse handleChatbotGeneralException(ChatbotGeneralException exception) {
        return new ChatbotErrorResponse(LocalDateTime.now(), exception.getMessage());
    }

    /*
        Fallback. Sem ele, uma exceção não mapeada só vira log no servidor e o cliente fica
        esperando uma resposta que nunca chega.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = ChatbotService.ERRORS_QUEUE_PATH, broadcast = false)
    public ChatbotErrorResponse handleGenericException(Exception exception) {
        log.error("Unhandled exception from chatbot (STOMP level)", exception);
        Optional<InternalLog> savedInternalLog = internalLogService.getInternalLog(exception);
        return new ChatbotErrorResponse(
                LocalDateTime.now(),
                getGenericMessage(savedInternalLog)
        );
    }

    private String getGenericMessage(Optional<InternalLog> internalLogOptional) {
        String reasonPlusDot = internalLogService.unhandledExceptionReason(internalLogOptional) + ".";
        String messageWithReason = "Mensagem do Servidor: " + reasonPlusDot;
        
        if (internalLogOptional.isPresent()) {
            return "Erro interno ao processar a sua mensagem. Tente novamente ou informe o ID " +
                internalLogOptional.get().getId() + " no formulário de contato do site. " + messageWithReason;
        }

        return "Erro interno ao processar a sua mensagem.";
    }
}
