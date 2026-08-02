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

    private static final String RESPOSTA_DA_IA = "O Felipe tem projetos em Java e Angular.";
    private static final int TIMEOUT_SEGUNDOS = 15;

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

    // ─────────────────────────── infraestrutura do teste ───────────────────────────

    private String urlDoEndpoint() {
        return "ws://localhost:" + port + "/api/websocket";
    }

    private WebSocketStompClient clienteStomp() {
        WebSocketStompClient cliente = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        cliente.setMessageConverter(new JacksonJsonMessageConverter());
        // Sem heartbeat: evita exigir um TaskScheduler só para o teste.
        cliente.setDefaultHeartbeat(new long[] {0, 0});
        return cliente;
    }

    private String forjarTokenPara(String sub, String preferredUsername) {
        Jwt jwt = Jwt.withTokenValue("token-de-teste")
                .header("alg", "none")
                .claim("sub", sub)
                .claim("preferred_username", preferredUsername)
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
        return "token-de-teste";
    }

    private StompHeaders headersComToken(String token) {
        StompHeaders headers = new StompHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    private StompSession conectar(StompHeaders connectHeaders, HandlerDeSessao handler) throws Exception {
        return clienteStomp()
                .connectAsync(urlDoEndpoint(), new WebSocketHttpHeaders(), connectHeaders, handler)
                .get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> assinar(StompSession sessao, String destino) {
        BlockingQueue<Map<String, Object>> fila = new LinkedBlockingQueue<>();
        sessao.subscribe(destino, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                fila.add((Map<String, Object>) payload);
            }
        });
        return fila;
    }

    private Map<String, Object> receber(BlockingQueue<Map<String, Object>> fila) throws InterruptedException {
        return fila.poll(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
    }

    // ───────────────────────────────── cenários ─────────────────────────────────

    @Test
    @DisplayName("CONNECT sem token deve ser recusado")
    void connectSemTokenDeveSerRecusado() {
        forjarTokenPara(UUID.randomUUID().toString(), "felipe");

        /*
            Regressão: com o handshake liberado para o SockJS funcionar, a única barreira
            é a autenticação no frame CONNECT. Se ela cair, o chat vira gasto de token
            aberto para qualquer visitante.
         */
        assertThrows(
                ExecutionException.class,
                () -> conectar(new StompHeaders(), new HandlerDeSessao())
        );
    }

    @Test
    @DisplayName("CONNECT com token invalido deve ser recusado")
    void connectComTokenInvalidoDeveSerRecusado() {
        when(jwtDecoder.decode(anyString()))
                .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("token invalido"));

        assertThrows(
                ExecutionException.class,
                () -> conectar(headersComToken("qualquer-coisa"), new HandlerDeSessao())
        );
    }

    @Test
    @DisplayName("Fluxo completo: eco da propria mensagem, aviso de processamento e resposta da IA")
    void fluxoCompletoDeveEntregarAsTresMensagens() throws Exception {
        String token = forjarTokenPara(UUID.randomUUID().toString(), "felipe");
        when(chatClient.prompt(anyString()).call().content()).thenReturn(RESPOSTA_DA_IA);

        StompSession sessao = conectar(headersComToken(token), new HandlerDeSessao());
        BlockingQueue<Map<String, Object>> mensagens = assinar(sessao, "/user" + ChatbotService.FILA_MENSAGENS);
        BlockingQueue<Map<String, Object>> respostas = assinar(sessao, "/user" + ChatbotService.FILA_RESPOSTAS);

        sessao.send("/chatbot/new-message", Map.of("message", "Quais projetos o Felipe tem?"));

        Map<String, Object> eco = receber(mensagens);
        assertNotNull(eco, "A propria mensagem nao voltou para o usuario");
        assertEquals("felipe", eco.get("name"));
        assertEquals("Quais projetos o Felipe tem?", eco.get("message"));

        Map<String, Object> processando = receber(respostas);
        assertNotNull(processando);
        assertEquals("sistema", processando.get("name"));

        Map<String, Object> respostaIA = receber(respostas);
        assertNotNull(respostaIA, "A resposta da IA nao chegou");
        assertEquals("bot", respostaIA.get("name"));
        assertEquals(RESPOSTA_DA_IA, respostaIA.get("message"));

        sessao.disconnect();
    }

    @Test
    @DisplayName("Payload invalido deve virar mensagem na fila de erros")
    void payloadInvalidoDeveVirarMensagemNaFilaDeErros() throws Exception {
        String token = forjarTokenPara(UUID.randomUUID().toString(), "felipe");

        StompSession sessao = conectar(headersComToken(token), new HandlerDeSessao());
        BlockingQueue<Map<String, Object>> erros = assinar(sessao, "/user" + ChatbotService.FILA_ERROS);

        sessao.send("/chatbot/new-message", Map.of("message", "   "));

        /*
            Regressão: antes o handler importava a MethodArgumentNotValidException do
            org.springframework.web.bind, que não tem relação com a lançada pelo messaging.
            O handler nunca disparava e o cliente ficava esperando para sempre.
         */
        Map<String, Object> erro = receber(erros);
        assertNotNull(erro, "A falha de validacao nao chegou ao cliente");
        assertEquals("Mensagem inválida!", erro.get("message"));

        sessao.disconnect();
    }

    @Test
    @DisplayName("Mensagem com URL deve virar mensagem na fila de erros")
    void mensagemComUrlDeveVirarMensagemNaFilaDeErros() throws Exception {
        String token = forjarTokenPara(UUID.randomUUID().toString(), "felipe");

        StompSession sessao = conectar(headersComToken(token), new HandlerDeSessao());
        BlockingQueue<Map<String, Object>> erros = assinar(sessao, "/user" + ChatbotService.FILA_ERROS);

        sessao.send("/chatbot/new-message", Map.of("message", "Veja &#104;ttps://site-malicioso.com"));

        Map<String, Object> erro = receber(erros);
        assertNotNull(erro);
        assertTrue(String.valueOf(erro.get("message")).contains("URL"));

        sessao.disconnect();
    }

    @Test
    @DisplayName("SUBSCRIBE em fila crua (sem /user) deve ser recusado")
    void subscribeEmFilaCruaDeveSerRecusado() throws Exception {
        String token = forjarTokenPara(UUID.randomUUID().toString(), "felipe");

        HandlerDeSessao handler = new HandlerDeSessao();
        StompSession sessao = conectar(headersComToken(token), handler);

        // Sem o /user na frente, a fila é compartilhada: qualquer um leria a resposta
        // destinada a outra pessoa.
        assinar(sessao, ChatbotService.FILA_RESPOSTAS);

        assertTrue(
                handler.aguardarErro(TIMEOUT_SEGUNDOS),
                "O SUBSCRIBE em fila compartilhada deveria ter sido recusado"
        );
    }

    @Test
    @DisplayName("Estourar a cota deve devolver erro sem derrubar a sessao")
    void estourarACotaDeveDevolverErroSemDerrubarASessao() throws Exception {
        String token = forjarTokenPara(UUID.randomUUID().toString(), "felipe");
        when(chatClient.prompt(anyString()).call().content()).thenReturn(RESPOSTA_DA_IA);

        HandlerDeSessao handler = new HandlerDeSessao();
        StompSession sessao = conectar(headersComToken(token), handler);
        BlockingQueue<Map<String, Object>> mensagens = assinar(sessao, "/user" + ChatbotService.FILA_MENSAGENS);
        BlockingQueue<Map<String, Object>> erros = assinar(sessao, "/user" + ChatbotService.FILA_ERROS);

        // O application.yml de teste define max-por-minuto = 3.
        for (int i = 0; i < 4; i++) {
            sessao.send("/chatbot/new-message", Map.of("message", "Pergunta numero " + i));
        }

        for (int i = 0; i < 3; i++) {
            assertNotNull(receber(mensagens), "A mensagem " + i + " deveria ter passado pela cota");
        }

        Map<String, Object> erro = receber(erros);
        assertNotNull(erro, "A quarta mensagem deveria ter sido barrada pela cota");
        assertTrue(String.valueOf(erro.get("message")).contains("Muitas perguntas seguidas"));

        // Cota estourada não pode derrubar a conexão do usuário.
        assertNull(handler.erro.get(), "A sessao nao deveria ter sido encerrada por estouro de cota");
        assertTrue(sessao.isConnected());
        assertFalse(mensagens.size() > 3, "A quarta mensagem nao poderia ter sido processada");

        sessao.disconnect();
    }

    /** Captura erro de transporte ou frame ERROR devolvido pelo servidor. */
    private static final class HandlerDeSessao extends StompSessionHandlerAdapter {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<Throwable> erro = new AtomicReference<>();

        private boolean aguardarErro(long segundos) throws InterruptedException {
            return latch.await(segundos, TimeUnit.SECONDS);
        }

        @Override
        public void handleException(
                StompSession session,
                org.springframework.messaging.simp.stomp.StompCommand command,
                StompHeaders headers,
                byte[] payload,
                Throwable exception
        ) {
            erro.set(exception);
            latch.countDown();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            erro.set(exception);
            latch.countDown();
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            // Frame ERROR devolvido pelo servidor cai aqui quando não há subscription.
            erro.set(new IllegalStateException("Frame de erro recebido"));
            latch.countDown();
        }
    }
}
