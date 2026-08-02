package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.ChatbotOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * Regras de envio das mensagens do chat.
 *
 * <p>O destino é sempre derivado do {@code Principal} da sessão STOMP (o {@code sub} do JWT
 * do Keycloak), nunca de algo vindo no payload. A versão anterior usava o nome digitado
 * pelo próprio usuário como destino, o que além de falsificável não entregava nada: o
 * {@code UserDestinationResolver} só entrega para sessões cujo {@code Principal} bata.
 */
@Service
@Slf4j
public class ChatbotService {

    public static final String FILA_MENSAGENS = "/queue/chatbot/mensagens";
    public static final String FILA_RESPOSTAS = "/queue/chatbot/respostas";
    public static final String FILA_ERROS = "/queue/chatbot/erros";

    private static final String REMETENTE_SISTEMA = "sistema";
    private static final String REMETENTE_BOT = "bot";
    private static final String AVISO_PROCESSANDO = "Mensagem em processamento..";
    private static final String ERRO_RESPOSTA = "Não consegui responder agora. Tente novamente em instantes.";

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatClient chatClient;
    private final Executor chatbotExecutor;

    public ChatbotService(
            SimpMessagingTemplate simpMessagingTemplate,
            ChatClient chatClient,
            @Qualifier("chatbotExecutor") Executor chatbotExecutor
    ) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.chatClient = chatClient;
        this.chatbotExecutor = chatbotExecutor;
    }

    /**
     * Devolve o eco da própria mensagem e o aviso de processamento de forma síncrona (são
     * baratos e garantem a ordem), e só a chamada paga à IA vai para o executor limitado.
     *
     * <p>A versão anterior abria um {@code ExecutorService} por mensagem dentro de um
     * try-with-resources. Em Java 21 o {@code close()} espera TODAS as tasks terminarem,
     * inclusive a chamada à Anthropic — ou seja, a thread do broker ficava travada durante
     * toda a requisição à IA, anulando justamente o assíncrono que se queria.
     */
    public void enviarMensagens(String usuario, String nomeExibicao, String pergunta) {
        LocalDateTime agora = LocalDateTime.now();

        enviar(usuario, FILA_MENSAGENS, new ChatbotOutput(nomeExibicao, pergunta, agora));
        enviar(usuario, FILA_RESPOSTAS, new ChatbotOutput(REMETENTE_SISTEMA, AVISO_PROCESSANDO, agora));

        log.info("Submetendo a pergunta de {} para o modelo de IA", usuario);
        chatbotExecutor.execute(() -> enviar(usuario, FILA_RESPOSTAS, obterRespostaIA(pergunta)));
    }

    private ChatbotOutput obterRespostaIA(String pergunta) {
        try {
            String resposta = chatClient.prompt(pergunta).call().content();
            return new ChatbotOutput(REMETENTE_BOT, resposta, LocalDateTime.now());
        } catch (Exception respostaIAException) {
            // Registrar de verdade: o catch anterior engolia o erro sem log nenhum.
            log.error("Falha ao obter a resposta do modelo de IA", respostaIAException);
            return new ChatbotOutput(REMETENTE_BOT, ERRO_RESPOSTA, LocalDateTime.now());
        }
    }

    private void enviar(String usuario, String fila, ChatbotOutput saida) {
        simpMessagingTemplate.convertAndSendToUser(usuario, fila, saida);
    }
}
