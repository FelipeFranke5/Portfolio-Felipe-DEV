package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.service.ChatbotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Teste ponta a ponta do chat: handshake, autenticação no CONNECT, roteamento por
 * {@code Principal}, validação, cota e autorização de destino.
 *
 * <p>Dois cuidados importantes: este teste NÃO pode mockar o {@code SimpMessagingTemplate}
 * (senão nada é entregue de verdade), e o {@code JwtDecoder} mockado — padrão já usado nos
 * demais testes de integração — é o que permite forjar um token válido sem subir o Keycloak.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChatbotWebSocketIntegrationTest {

    private static final String AI_MOCK_RESPONSE = "O Felipe tem projetos em Java e Angular.";
    private static final int TIMEOUT_IN_SECONDS = 15;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.0");

    @LocalServerPort
    private int port;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    /*
        Substitui o bean de verdade — nenhum teste alcança a Anthropic. Isso só passou a
        funcionar quando o ChatClient virou bean próprio: antes o service injetava o
        ChatClient.Builder, e o mock não substituía nada.
     */
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private String websocketEndpointUrl() {
        return "ws://localhost:" + port + "/api/websocket";
    }

    private WebSocketStompClient stompClient() {
        WebSocketStompClient webSocketStompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        webSocketStompClient.setMessageConverter(new JacksonJsonMessageConverter());
        // Sem heartbeat: evita exigir um TaskScheduler só para o teste.
        webSocketStompClient.setDefaultHeartbeat(new long[] {0, 0});
        return webSocketStompClient;
    }

    private String mockToken(String sub, String preferredUsername) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", sub)
                .claim("preferred_username", preferredUsername)
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
        return "test-token";
    }

    private StompHeaders getStompHeaders(String token) {
        StompHeaders headers = new StompHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    private StompSession getStompSession(StompHeaders connectHeaders, SessionHandler handler) throws Exception {
        return stompClient()
                .connectAsync(websocketEndpointUrl(), new WebSocketHttpHeaders(), connectHeaders, handler)
                .get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> subscribeToSession(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((Map<String, Object>) payload);
            }
        });
        return queue;
    }

    private Map<String, Object> pollFromQueue(BlockingQueue<Map<String, Object>> queue) throws InterruptedException {
        return queue.poll(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("CONNECT sent without a token should be rejected")
    void connectRequestWithoutTokenShouldBeRejected() {
        assertThrows(
                ExecutionException.class,
                () -> getStompSession(new StompHeaders(), new SessionHandler())
        );
    }

    @Test
    @DisplayName("CONNECT sent with invalid token should be rejected")
    void connectRequestWithInvalidTokenShouldBeRejected() {
        when(jwtDecoder.decode(anyString()))
                .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("invalid token"));

        assertThrows(
                ExecutionException.class,
                () -> getStompSession(getStompHeaders("anything"), new SessionHandler())
        );
    }

    @Test
    @DisplayName(
        "When the user sends a message, 3 messages should be sent " +
        "back (own message, automatic message and ai response)"
    )
    void userSendsMessageShouldReceiveAutomaticMessageAndAIResponse() throws Exception {
        String token = mockToken(UUID.randomUUID().toString(), "felipe");
        when(chatClient.prompt(anyString()).call().content()).thenReturn(AI_MOCK_RESPONSE);

        StompSession session = getStompSession(getStompHeaders(token), new SessionHandler());
        BlockingQueue<Map<String, Object>> messages = subscribeToSession(session, "/user" + ChatbotService.MESSAGES_QUEUE_PATH);
        BlockingQueue<Map<String, Object>> responses = subscribeToSession(session, "/user" + ChatbotService.RESPONSES_QUEUE_PATH);

        session.send("/chatbot/new-message", Map.of("message", "Quais projetos o Felipe tem?"));

        Map<String, Object> ownMessageEcho = pollFromQueue(messages);
        assertNotNull(ownMessageEcho, "User's own message did not return to him");
        assertEquals("felipe", ownMessageEcho.get("name"));
        assertEquals("Quais projetos o Felipe tem?", ownMessageEcho.get("message"));

        Map<String, Object> automaticMessageEcho = pollFromQueue(responses);
        assertNotNull(automaticMessageEcho);
        assertEquals("sistema", automaticMessageEcho.get("name"));

        Map<String, Object> aiResponseEcho = pollFromQueue(responses);
        assertNotNull(aiResponseEcho, "AI response did not return to user");
        assertEquals("bot", aiResponseEcho.get("name"));
        assertEquals(AI_MOCK_RESPONSE, aiResponseEcho.get("message"));

        session.disconnect();
    }

    @Test
    @DisplayName("When user sends invalid payload, error queue should receive the error message")
    void userSendsInvalidPayloadShouldPopulateErrorQueue() throws Exception {
        String token = mockToken(UUID.randomUUID().toString(), "felipe");

        StompSession session = getStompSession(getStompHeaders(token), new SessionHandler());
        BlockingQueue<Map<String, Object>> errors = subscribeToSession(session, "/user" + ChatbotService.ERRORS_QUEUE_PATH);

        session.send("/chatbot/new-message", Map.of("message", "   "));

        Map<String, Object> errorEcho = pollFromQueue(errors);
        assertNotNull(errorEcho, "Validation Error did NOT reach the user");
        assertEquals("Mensagem inválida!", errorEcho.get("message"));

        session.disconnect();
    }

    @Test
    @DisplayName("When user sends message with URL, error queue should receive the error message")
    void userSendsURLShouldPopulateErrorQueue() throws Exception {
        String token = mockToken(UUID.randomUUID().toString(), "felipe");

        StompSession session = getStompSession(getStompHeaders(token), new SessionHandler());
        BlockingQueue<Map<String, Object>> errors = subscribeToSession(session, "/user" + ChatbotService.ERRORS_QUEUE_PATH);

        session.send("/chatbot/new-message", Map.of("message", "Veja &#104;ttps://site-malicioso.com"));

        Map<String, Object> errorEcho = pollFromQueue(errors);
        assertNotNull(errorEcho);
        assertTrue(String.valueOf(errorEcho.get("message")).contains("URL"));

        session.disconnect();
    }

    @Test
    @DisplayName("SUBSCRIBE request without /user should be rejected")
    void subscribeRequestWithoutUserPathShouldBeRejected() throws Exception {
        String token = mockToken(UUID.randomUUID().toString(), "felipe");

        SessionHandler handler = new SessionHandler();
        StompSession session = getStompSession(getStompHeaders(token), handler);

        subscribeToSession(session, ChatbotService.RESPONSES_QUEUE_PATH);

        assertTrue(
                handler.waitForError(TIMEOUT_IN_SECONDS),
                "SUBSCRIBE request without /user path was supposed to be rejected, but it wasn't"
        );
    }

    @Test
    @DisplayName(
        "When the user exceeds rate limit, error queue should receive " +
        "the error message without disconnecting"
    )
    void userExceedsQuotaShouldPopulateErrorQueue() throws Exception {
        String token = mockToken(UUID.randomUUID().toString(), "felipe");
        when(chatClient.prompt(anyString()).call().content()).thenReturn(AI_MOCK_RESPONSE);

        SessionHandler handler = new SessionHandler();
        StompSession session = getStompSession(getStompHeaders(token), handler);
        BlockingQueue<Map<String, Object>> messages = subscribeToSession(session, "/user" + ChatbotService.MESSAGES_QUEUE_PATH);
        BlockingQueue<Map<String, Object>> errors = subscribeToSession(session, "/user" + ChatbotService.ERRORS_QUEUE_PATH);

        // Em DEV, o maxPerMinute está definido para 3 (valor fixo)
        int maxPerMinuteLimit = 3;
        int maxPerMinuteLimitPlusOne = maxPerMinuteLimit + 1;

        for (int mockUserMessageCount = 0; mockUserMessageCount < maxPerMinuteLimitPlusOne; mockUserMessageCount++) {
            session.send("/chatbot/new-message", Map.of("message", "Pergunta numero " + mockUserMessageCount));
        }

        for (int mockUserMessageCount = 0; mockUserMessageCount < maxPerMinuteLimit; mockUserMessageCount++) {
            assertNotNull(
                pollFromQueue(messages), 
                "Message count " + mockUserMessageCount + " did NOT reach the user's maxPerMinute " +
                "limit, but the message is not present"
            );
        }

        Map<String, Object> errorEcho = pollFromQueue(errors);
        assertNotNull(errorEcho, "4th message should be blocked - Check Rate Limit logic");
        assertTrue(String.valueOf(errorEcho.get("message")).contains("Muitas perguntas seguidas"));

        // Cota estourada não pode derrubar a conexão do usuário.
        assertNull(handler.error.get(), "The session is not supposed to be terminated");
        assertTrue(session.isConnected());
        assertFalse(messages.size() > 3, "4th message is not supposed to be processed");

        session.disconnect();
    }

    /** Captura erro de transporte ou frame ERROR devolvido pelo servidor. */
    private static final class SessionHandler extends StompSessionHandlerAdapter {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        private boolean waitForError(long seconds) throws InterruptedException {
            return latch.await(seconds, TimeUnit.SECONDS);
        }

        @Override
        public void handleException(
                StompSession session,
                org.springframework.messaging.simp.stomp.StompCommand command,
                StompHeaders headers,
                byte[] payload,
                Throwable exception
        ) {
            error.set(exception);
            latch.countDown();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            error.set(exception);
            latch.countDown();
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            // Frame ERROR devolvido pelo servidor cai aqui quando não há subscription.
            error.set(new IllegalStateException("Received error Frame"));
            latch.countDown();
        }
    }
}
