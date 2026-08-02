package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.config.ChatbotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatbotRateLimiterTest {

    private static final String USUARIO = "usuario-a";
    private static final String OUTRO_USUARIO = "usuario-b";

    private RelogioAjustavel relogio;
    private ChatbotRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        relogio = new RelogioAjustavel(Instant.parse("2026-08-01T10:00:00Z"));
        rateLimiter = new ChatbotRateLimiter(new ChatbotProperties(3, 5, 2), relogio);
    }

    @Test
    @DisplayName("Deve permitir ate o limite por minuto e barrar a proxima")
    void deveBarrarAcimaDoLimitePorMinuto() {
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
        assertEquals(ChatbotRateLimiter.Decisao.EXCEDEU_POR_MINUTO, rateLimiter.tentarConsumir(USUARIO));
    }

    @Test
    @DisplayName("Deve liberar novamente depois que a janela desliza")
    void deveLiberarDepoisDaJanela() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tentarConsumir(USUARIO);
        }
        assertEquals(ChatbotRateLimiter.Decisao.EXCEDEU_POR_MINUTO, rateLimiter.tentarConsumir(USUARIO));

        relogio.avancar(Duration.ofSeconds(61));
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
    }

    @Test
    @DisplayName("Deve barrar ao estourar a cota diaria e liberar no dia seguinte")
    void deveBarrarAoEstourarACotaDiaria() {
        // 5 no dia, espacando para nao esbarrar no limite por minuto
        for (int i = 0; i < 5; i++) {
            assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
            relogio.avancar(Duration.ofSeconds(61));
        }
        assertEquals(ChatbotRateLimiter.Decisao.EXCEDEU_COTA_DIARIA, rateLimiter.tentarConsumir(USUARIO));

        relogio.avancar(Duration.ofDays(1));
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
    }

    @Test
    @DisplayName("O limite de um usuario nao deve afetar outro")
    void oLimiteDeUmUsuarioNaoDeveAfetarOutro() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tentarConsumir(USUARIO);
        }
        assertEquals(ChatbotRateLimiter.Decisao.EXCEDEU_POR_MINUTO, rateLimiter.tentarConsumir(USUARIO));
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(OUTRO_USUARIO));
    }

    @Test
    @DisplayName("Mensagem barrada nao deve consumir a cota diaria")
    void mensagemBarradaNaoDeveConsumirACotaDiaria() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tentarConsumir(USUARIO);
        }
        // Estoura o limite por minuto varias vezes: nenhuma delas pode contar no dia.
        for (int i = 0; i < 10; i++) {
            assertEquals(ChatbotRateLimiter.Decisao.EXCEDEU_POR_MINUTO, rateLimiter.tentarConsumir(USUARIO));
        }

        relogio.avancar(Duration.ofSeconds(61));
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
        relogio.avancar(Duration.ofSeconds(61));
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
    }

    @Test
    @DisplayName("Deve esquecer usuarios ociosos, para o mapa nao crescer sem limite")
    void deveEsquecerUsuariosOciosos() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tentarConsumir(USUARIO);
        }
        assertEquals(ChatbotRateLimiter.Decisao.EXCEDEU_POR_MINUTO, rateLimiter.tentarConsumir(USUARIO));

        relogio.avancar(ChatbotRateLimiter.TTL_USUARIO_OCIOSO.plusMinutes(1));
        rateLimiter.limparUsuariosOciosos();

        // Estado zerado: volta a aceitar as 3 do minuto.
        assertEquals(ChatbotRateLimiter.Decisao.PERMITIDO, rateLimiter.tentarConsumir(USUARIO));
    }

    /** Relógio controlado pelo teste — sem isso, testar a janela exigiria Thread.sleep. */
    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        private RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        private void avancar(Duration duracao) {
            this.agora = this.agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
