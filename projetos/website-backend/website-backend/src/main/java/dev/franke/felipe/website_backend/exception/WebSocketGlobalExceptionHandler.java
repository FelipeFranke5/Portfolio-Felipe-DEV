package dev.franke.felipe.website_backend.exception;

import dev.franke.felipe.website_backend.dto.ChatbotErrorResponse;
import dev.franke.felipe.website_backend.service.ChatbotService;
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

/**
 * Traduz as falhas do fluxo STOMP em mensagens na fila de erro do próprio usuário.
 *
 * <p>{@code broadcast = false} é obrigatório aqui: com o padrão {@code true}, o erro de uma
 * aba iria para todas as sessões abertas daquele usuário.
 */
@ControllerAdvice
@Slf4j
public class WebSocketGlobalExceptionHandler {

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = ChatbotService.FILA_ERROS, broadcast = false)
    public ChatbotErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        log.info("Payload invalido recebido no chatbot");
        return new ChatbotErrorResponse(LocalDateTime.now(), "Mensagem inválida!");
    }

    @MessageExceptionHandler(ChatbotRateLimitException.class)
    @SendToUser(destinations = ChatbotService.FILA_ERROS, broadcast = false)
    public ChatbotErrorResponse handleChatbotRateLimitException(ChatbotRateLimitException exception) {
        return new ChatbotErrorResponse(LocalDateTime.now(), exception.getMessage());
    }

    @MessageExceptionHandler(ChatbotGeneralException.class)
    @SendToUser(destinations = ChatbotService.FILA_ERROS, broadcast = false)
    public ChatbotErrorResponse handleChatbotGeneralException(ChatbotGeneralException exception) {
        return new ChatbotErrorResponse(LocalDateTime.now(), exception.getMessage());
    }

    /*
        Fallback. Sem ele, uma exceção não mapeada só vira log no servidor e o cliente fica
        esperando uma resposta que nunca chega.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = ChatbotService.FILA_ERROS, broadcast = false)
    public ChatbotErrorResponse handleGenericException(Exception exception) {
        log.error("Excecao nao tratada no fluxo STOMP do chatbot", exception);
        return new ChatbotErrorResponse(
                LocalDateTime.now(),
                "Erro interno ao processar a sua mensagem. Tente novamente."
        );
    }
}
