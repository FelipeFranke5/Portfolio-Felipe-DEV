package dev.franke.felipe.website_backend.exception;

import dev.franke.felipe.website_backend.dto.ChatbotErrorResponse;
import dev.franke.felipe.website_backend.service.ChatbotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketGlobalExceptionHandlerTest {

    private final WebSocketGlobalExceptionHandler handler = new WebSocketGlobalExceptionHandler();

    @Test
    @DisplayName("Deve tratar a MethodArgumentNotValidException do messaging, nao a do web")
    void deveTratarAExcecaoDoMessagingNaoADoWeb() throws NoSuchMethodException {
        /*
            Regressao do bug mais silencioso da implementacao anterior: o handler importava
            org.springframework.web.bind.MethodArgumentNotValidException. As duas classes tem
            o mesmo nome simples, mas nao tem relacao de heranca — o Spring Messaging lanca a
            do pacote .messaging.handler.annotation.support, entao o handler nunca disparava e
            o cliente ficava esperando para sempre.
         */
        Method metodo = WebSocketGlobalExceptionHandler.class.getDeclaredMethod(
                "handleMethodArgumentNotValidException",
                org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException.class
        );

        MessageExceptionHandler anotacao = metodo.getAnnotation(MessageExceptionHandler.class);
        assertNotNull(anotacao);
        assertTrue(Arrays.asList(anotacao.value())
                .contains(org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException.class));
    }

    @Test
    @DisplayName("Todo handler deve responder na fila individual, sem broadcast")
    void todoHandlerDeveResponderNaFilaIndividual() {
        /*
            Com o broadcast = true padrao, o erro de uma aba iria para todas as sessoes
            abertas daquele usuario.
         */
        long total = 0;
        for (Method metodo : WebSocketGlobalExceptionHandler.class.getDeclaredMethods()) {
            if (metodo.getAnnotation(MessageExceptionHandler.class) == null) {
                continue;
            }
            total++;
            SendToUser sendToUser = metodo.getAnnotation(SendToUser.class);
            assertNotNull(sendToUser, "Handler sem @SendToUser: " + metodo.getName());
            assertFalse(sendToUser.broadcast(), "Handler com broadcast ligado: " + metodo.getName());
            assertEquals(ChatbotService.FILA_ERROS, sendToUser.destinations()[0]);
        }
        assertEquals(4, total, "Esperado um handler para validacao, cota, regra de negocio e fallback");
    }

    @Test
    @DisplayName("Deve devolver a mensagem da excecao de cota para o usuario")
    void deveDevolverAMensagemDaExcecaoDeCota() {
        ChatbotErrorResponse resposta = handler.handleChatbotRateLimitException(
                new ChatbotRateLimitException("Muitas perguntas seguidas.")
        );

        assertEquals("Muitas perguntas seguidas.", resposta.message());
        assertNotNull(resposta.currentTime());
    }

    @Test
    @DisplayName("Deve devolver a mensagem da excecao de regra de negocio")
    void deveDevolverAMensagemDaExcecaoDeNegocio() {
        ChatbotErrorResponse resposta = handler.handleChatbotGeneralException(
                new ChatbotGeneralException("A mensagem não pode conter URL(s) ou domínio(s)!")
        );

        assertEquals("A mensagem não pode conter URL(s) ou domínio(s)!", resposta.message());
    }

    @Test
    @DisplayName("Fallback nao deve vazar detalhe interno da excecao")
    void fallbackNaoDeveVazarDetalheInterno() {
        ChatbotErrorResponse resposta = handler.handleGenericException(
                new IllegalStateException("senha=123 no datasource")
        );

        assertFalse(resposta.message().contains("senha=123"));
        assertTrue(resposta.message().contains("Erro interno"));
    }

    @Test
    @DisplayName("Validacao deve virar mensagem generica")
    void validacaoDeveVirarMensagemGenerica() {
        ChatbotErrorResponse resposta = handler.handleMethodArgumentNotValidException(null);

        assertEquals("Mensagem inválida!", resposta.message());
    }
}
