package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.ChatbotOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    private static final String MOCK_USER = "sub-do-keycloak-123";
    private static final String MOCK_DISPLAY_NAME = "felipe";
    private static final String MOCK_QUESTION = "Quais projetos o Felipe tem?";

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        // Executor sincrono: torna o teste deterministico sem sleep nem Awaitility.
        chatbotService = new ChatbotService(simpMessagingTemplate, chatClient, Runnable::run);
    }

    @Test
    @DisplayName("sendMessages should send messages in correct order")
    void sendMessagesShouldSendMessagesInCorrectOrder() {
        when(chatClient.prompt(anyString()).call().content()).thenReturn("O Felipe tem 3 projetos.");

        chatbotService.sendMessages(MOCK_USER, MOCK_DISPLAY_NAME, MOCK_QUESTION);

        ArgumentCaptor<ChatbotOutput> captor = ArgumentCaptor.forClass(ChatbotOutput.class);
        InOrder order = inOrder(simpMessagingTemplate);
        order.verify(simpMessagingTemplate)
                .convertAndSendToUser(eq(MOCK_USER), eq(ChatbotService.MESSAGES_QUEUE_PATH), any(ChatbotOutput.class));
        order.verify(simpMessagingTemplate, times(2))
                .convertAndSendToUser(eq(MOCK_USER), eq(ChatbotService.RESPONSES_QUEUE_PATH), any(ChatbotOutput.class));

        verify(simpMessagingTemplate, times(3))
                .convertAndSendToUser(eq(MOCK_USER), anyString(), captor.capture());

        List<ChatbotOutput> sentMessages = captor.getAllValues();
        assertEquals(MOCK_DISPLAY_NAME, sentMessages.get(0).name());
        assertEquals(MOCK_QUESTION, sentMessages.get(0).message());
        assertEquals("sistema", sentMessages.get(1).name());
        assertEquals("bot", sentMessages.get(2).name());
        assertEquals("O Felipe tem 3 projetos.", sentMessages.get(2).message());
    }

    @Test
    @DisplayName("sendMessages should always rely on Principal to retrieve the User's Display Name")
    void sendMessagesShouldAlwaysRelyOnPrincipalToRetrieveDisplayName() {
        when(chatClient.prompt(anyString()).call().content()).thenReturn("resposta");

        chatbotService.sendMessages(MOCK_USER, "nome-que-o-usuario-digitou", MOCK_QUESTION);

        verify(simpMessagingTemplate, times(3))
                .convertAndSendToUser(eq(MOCK_USER), anyString(), any(ChatbotOutput.class));
    }

    @Test
    @DisplayName("When AI response fails, the User should receive a friendly message")
    void whenAICallFailsUserShouldReceiveFriendlyMessage() {
        when(chatClient.prompt(anyString())).thenThrow(new IllegalStateException("provedor fora do ar"));

        chatbotService.sendMessages(MOCK_USER, MOCK_DISPLAY_NAME, MOCK_QUESTION);

        ArgumentCaptor<ChatbotOutput> captor = ArgumentCaptor.forClass(ChatbotOutput.class);
        verify(simpMessagingTemplate, times(3))
                .convertAndSendToUser(eq(MOCK_USER), anyString(), captor.capture());

        ChatbotOutput finalResponse = captor.getAllValues().get(2);
        assertEquals("bot", finalResponse.name());
        assertTrue(finalResponse.message().contains("Não consegui responder agora! Por favor, tente novamente mais tarde."));
    }

    @Test
    @DisplayName("When the User sends a message, the AI should be called only once")
    void whenUserSendsMessageAIShouldBeCalledOnce() {
        when(chatClient.prompt(anyString()).call().content()).thenReturn("resposta");

        chatbotService.sendMessages(MOCK_USER, MOCK_DISPLAY_NAME, MOCK_QUESTION);

        verify(chatClient, times(1)).prompt(MOCK_QUESTION);
    }
}
