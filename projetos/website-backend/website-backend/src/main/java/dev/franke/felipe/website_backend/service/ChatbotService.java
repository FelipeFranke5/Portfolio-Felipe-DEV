package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.ChatbotOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class ChatbotService {

    public static final String MESSAGES_QUEUE_PATH = "/queue/chatbot/mensagens";
    public static final String RESPONSES_QUEUE_PATH = "/queue/chatbot/respostas";
    public static final String ERRORS_QUEUE_PATH = "/queue/chatbot/erros";

    private static final String SYSTEM_AS_SENDER = "sistema";
    private static final String BOT_AS_SENDER = "bot";
    private static final String MESSAGE_BEING_PROCESSED_INDICATOR = "Mensagem em processamento..";
    private static final String MESSAGE_FAILED_INDICATOR = "Não consegui responder agora! Por favor, tente novamente mais tarde.";

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

    public void sendMessages(String user, String userDisplayName, String userQuestion) {
        Instant timeNow = Instant.now();

        send(user, MESSAGES_QUEUE_PATH, new ChatbotOutput(userDisplayName, userQuestion, timeNow));
        send(user, RESPONSES_QUEUE_PATH, new ChatbotOutput(SYSTEM_AS_SENDER, MESSAGE_BEING_PROCESSED_INDICATOR, timeNow));

        log.info("Sending userQuestion from {} to AI", user);
        chatbotExecutor.execute(() -> send(user, RESPONSES_QUEUE_PATH, retrieveAIResponse(userQuestion)));
    }

    private ChatbotOutput retrieveAIResponse(String userQuestion) {
        try {
            String aiResponse = chatClient.prompt(userQuestion).call().content();
            return new ChatbotOutput(BOT_AS_SENDER, aiResponse, Instant.now());
        } catch (Exception aiResponseException) {
            // Registrar de verdade: o catch anterior engolia o erro sem log nenhum.
            log.error("Unable to obtain response from AI", aiResponseException);
            return new ChatbotOutput(BOT_AS_SENDER, MESSAGE_FAILED_INDICATOR, Instant.now());
        }
    }

    private void send(String user, String queue_path, ChatbotOutput whatToSend) {
        simpMessagingTemplate.convertAndSendToUser(user, queue_path, whatToSend);
    }
}
