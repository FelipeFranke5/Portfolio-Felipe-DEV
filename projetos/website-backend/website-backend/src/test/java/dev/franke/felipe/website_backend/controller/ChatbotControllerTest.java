package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.dto.ChatbotInput;
import dev.franke.felipe.website_backend.exception.ChatbotGeneralException;
import dev.franke.felipe.website_backend.exception.ChatbotRateLimitException;
import dev.franke.felipe.website_backend.service.ChatbotInputSanitizer;
import dev.franke.felipe.website_backend.service.ChatbotRateLimiter;
import dev.franke.felipe.website_backend.service.ChatbotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotControllerTest {

    private static final String USUARIO = "sub-do-keycloak-123";

    @Mock
    private ChatbotService chatbotService;

    @Mock
    private ChatbotInputSanitizer chatbotInputSanitizer;

    @Mock
    private ChatbotRateLimiter chatbotRateLimiter;

    @InjectMocks
    private ChatbotController chatbotController;

    /** Reproduz o Principal que o interceptor coloca na sessão STOMP. */
    private Principal principal(String preferredUsername) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", USUARIO);
        if (preferredUsername != null) {
            builder.claim("preferred_username", preferredUsername);
        }
        return new JwtAuthenticationToken(builder.build(), List.of());
    }

    @Test
    @DisplayName("Deve sanitizar a mensagem e delegar para o service com o usuario da sessao")
    void deveDelegarParaOService() {
        when(chatbotRateLimiter.tentarConsumir(USUARIO)).thenReturn(ChatbotRateLimiter.Decisao.PERMITIDO);
        when(chatbotInputSanitizer.sanitizar("  Quais projetos?  ")).thenReturn("Quais projetos?");

        chatbotController.newMessage(new ChatbotInput("  Quais projetos?  "), principal("felipe"));

        verify(chatbotService).enviarMensagens(USUARIO, "felipe", "Quais projetos?");
    }

    @Test
    @DisplayName("Sem preferred_username no token, deve usar o nome de exibicao padrao")
    void deveUsarNomeDeExibicaoPadrao() {
        when(chatbotRateLimiter.tentarConsumir(USUARIO)).thenReturn(ChatbotRateLimiter.Decisao.PERMITIDO);
        when(chatbotInputSanitizer.sanitizar(anyString())).thenReturn("Quais projetos?");

        chatbotController.newMessage(new ChatbotInput("Quais projetos?"), principal(null));

        verify(chatbotService).enviarMensagens(eq(USUARIO), eq("você"), anyString());
    }

    @Test
    @DisplayName("Cota estourada deve barrar antes de qualquer trabalho")
    void cotaEstouradaDeveBarrarAntesDeQualquerTrabalho() {
        when(chatbotRateLimiter.tentarConsumir(USUARIO))
                .thenReturn(ChatbotRateLimiter.Decisao.EXCEDEU_POR_MINUTO);

        ChatbotRateLimitException excecao = assertThrows(
                ChatbotRateLimitException.class,
                () -> chatbotController.newMessage(new ChatbotInput("Quais projetos?"), principal("felipe"))
        );

        assertEquals(ChatbotRateLimiter.Decisao.EXCEDEU_POR_MINUTO.mensagem(), excecao.getMessage());
        // Nenhuma chamada paga pode acontecer depois do limite.
        verifyNoInteractions(chatbotService);
        verifyNoInteractions(chatbotInputSanitizer);
    }

    @Test
    @DisplayName("Mensagem recusada na sanitizacao nao deve chegar ao service")
    void mensagemRecusadaNaSanitizacaoNaoDeveChegarAoService() {
        when(chatbotRateLimiter.tentarConsumir(USUARIO)).thenReturn(ChatbotRateLimiter.Decisao.PERMITIDO);
        when(chatbotInputSanitizer.sanitizar(anyString()))
                .thenThrow(new ChatbotGeneralException("A mensagem não pode conter URL(s) ou domínio(s)!"));

        assertThrows(
                ChatbotGeneralException.class,
                () -> chatbotController.newMessage(new ChatbotInput("https://x.com"), principal("felipe"))
        );

        verify(chatbotService, never()).enviarMensagens(anyString(), anyString(), anyString());
    }
}
