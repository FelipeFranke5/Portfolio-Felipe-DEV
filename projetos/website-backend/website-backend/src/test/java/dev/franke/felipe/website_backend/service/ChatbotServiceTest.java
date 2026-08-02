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

    private static final String USUARIO = "sub-do-keycloak-123";
    private static final String NOME_EXIBICAO = "felipe";
    private static final String PERGUNTA = "Quais projetos o Felipe tem?";

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
    @DisplayName("Deve enviar eco, aviso de processamento e resposta da IA, nessa ordem")
    void deveEnviarAsTresMensagensNaOrdem() {
        when(chatClient.prompt(anyString()).call().content()).thenReturn("O Felipe tem 3 projetos.");

        chatbotService.enviarMensagens(USUARIO, NOME_EXIBICAO, PERGUNTA);

        ArgumentCaptor<ChatbotOutput> captor = ArgumentCaptor.forClass(ChatbotOutput.class);
        InOrder ordem = inOrder(simpMessagingTemplate);
        ordem.verify(simpMessagingTemplate)
                .convertAndSendToUser(eq(USUARIO), eq(ChatbotService.FILA_MENSAGENS), any(ChatbotOutput.class));
        ordem.verify(simpMessagingTemplate, times(2))
                .convertAndSendToUser(eq(USUARIO), eq(ChatbotService.FILA_RESPOSTAS), any(ChatbotOutput.class));

        verify(simpMessagingTemplate, times(3))
                .convertAndSendToUser(eq(USUARIO), anyString(), captor.capture());

        List<ChatbotOutput> enviadas = captor.getAllValues();
        assertEquals(NOME_EXIBICAO, enviadas.get(0).name());
        assertEquals(PERGUNTA, enviadas.get(0).message());
        assertEquals("sistema", enviadas.get(1).name());
        assertEquals("bot", enviadas.get(2).name());
        assertEquals("O Felipe tem 3 projetos.", enviadas.get(2).message());
    }

    @Test
    @DisplayName("Deve rotear pelo Principal da sessao, nunca por algo vindo do payload")
    void deveRotearPeloPrincipalDaSessao() {
        /*
            Regressao: a versao anterior usava o nome digitado pelo usuario como destino.
            Alem de falsificavel, nao entregava nada — o UserDestinationResolver so entrega
            para sessoes cujo Principal bata.
         */
        when(chatClient.prompt(anyString()).call().content()).thenReturn("resposta");

        chatbotService.enviarMensagens(USUARIO, "nome-que-o-usuario-digitou", PERGUNTA);

        verify(simpMessagingTemplate, times(3))
                .convertAndSendToUser(eq(USUARIO), anyString(), any(ChatbotOutput.class));
    }

    @Test
    @DisplayName("Falha da IA nao pode derrubar o fluxo: usuario recebe aviso amigavel")
    void falhaDaIaDeveVirarAvisoAmigavel() {
        when(chatClient.prompt(anyString())).thenThrow(new IllegalStateException("provedor fora do ar"));

        chatbotService.enviarMensagens(USUARIO, NOME_EXIBICAO, PERGUNTA);

        ArgumentCaptor<ChatbotOutput> captor = ArgumentCaptor.forClass(ChatbotOutput.class);
        verify(simpMessagingTemplate, times(3))
                .convertAndSendToUser(eq(USUARIO), anyString(), captor.capture());

        ChatbotOutput respostaFinal = captor.getAllValues().get(2);
        assertEquals("bot", respostaFinal.name());
        assertTrue(respostaFinal.message().contains("Não consegui responder agora"));
    }

    @Test
    @DisplayName("Deve chamar a IA exatamente uma vez por mensagem")
    void deveChamarAIaUmaUnicaVez() {
        // Trava de custo: cada pergunta e uma chamada paga.
        when(chatClient.prompt(anyString()).call().content()).thenReturn("resposta");

        chatbotService.enviarMensagens(USUARIO, NOME_EXIBICAO, PERGUNTA);

        verify(chatClient, times(1)).prompt(PERGUNTA);
    }
}
