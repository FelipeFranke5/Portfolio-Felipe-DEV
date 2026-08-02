package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.config.ChatbotProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controle de abuso do chatbot, em memória e por usuário autenticado.
 *
 * <p>Cada pergunta vira pelo menos uma chamada paga à Anthropic — e o loop de tool calling
 * costuma gerar mais de uma. Sem este limite, um único usuário logado consegue esvaziar o
 * saldo da API em um laço de repetição.
 *
 * <p>O estado vive só nesta instância. Com uma única réplica na VPS isso basta; se um dia
 * houver escala horizontal, o limite precisa migrar para um store compartilhado.
 */
@Service
@Slf4j
public class ChatbotRateLimiter {

    static final Duration JANELA = Duration.ofMinutes(1);
    static final Duration TTL_USUARIO_OCIOSO = Duration.ofHours(6);

    private final ChatbotProperties propriedades;
    private final Clock clock;
    private final Map<String, EstadoUsuario> porUsuario = new ConcurrentHashMap<>();

    public ChatbotRateLimiter(ChatbotProperties propriedades, Clock clock) {
        this.propriedades = propriedades;
        this.clock = clock;
    }

    /**
     * Registra uma pergunta do usuário e devolve a decisão. Os contadores só são
     * incrementados quando a mensagem é permitida.
     */
    public Decisao tentarConsumir(String usuario) {
        Instant agora = clock.instant();
        LocalDate hoje = LocalDate.ofInstant(agora, ZoneId.systemDefault());

        EstadoUsuario estado = porUsuario.computeIfAbsent(usuario, chave -> new EstadoUsuario(hoje));

        synchronized (estado) {
            estado.ultimoAcesso = agora;

            if (!hoje.equals(estado.dia)) {
                estado.dia = hoje;
                estado.contagemDia = 0;
            }

            Instant inicioJanela = agora.minus(JANELA);
            while (!estado.janela.isEmpty() && estado.janela.peekFirst().isBefore(inicioJanela)) {
                estado.janela.pollFirst();
            }

            if (estado.janela.size() >= propriedades.maxPorMinuto()) {
                log.info("Usuario {} estourou o limite por minuto do chatbot", usuario);
                return Decisao.EXCEDEU_POR_MINUTO;
            }

            if (estado.contagemDia >= propriedades.maxPorUsuarioPorDia()) {
                log.info("Usuario {} estourou a cota diaria do chatbot", usuario);
                return Decisao.EXCEDEU_COTA_DIARIA;
            }

            estado.janela.addLast(agora);
            estado.contagemDia++;
            return Decisao.PERMITIDO;
        }
    }

    /**
     * Sem esta limpeza o mapa cresceria indefinidamente conforme novos usuários aparecem,
     * o que é um vetor de exaustão de memória.
     */
    @Scheduled(fixedRate = 3_600_000)
    public void limparUsuariosOciosos() {
        Instant corte = clock.instant().minus(TTL_USUARIO_OCIOSO);
        porUsuario.entrySet().removeIf(entrada -> {
            EstadoUsuario estado = entrada.getValue();
            synchronized (estado) {
                return estado.ultimoAcesso.isBefore(corte);
            }
        });
    }

    public enum Decisao {

        PERMITIDO(null),
        EXCEDEU_POR_MINUTO("Muitas perguntas seguidas. Aguarde um instante antes de tentar novamente."),
        EXCEDEU_COTA_DIARIA("Você atingiu o limite diário de perguntas ao assistente. Tente novamente amanhã.");

        private final String mensagem;

        Decisao(String mensagem) {
            this.mensagem = mensagem;
        }

        public String mensagem() {
            return mensagem;
        }

        public boolean permitido() {
            return this == PERMITIDO;
        }
    }

    private static final class EstadoUsuario {

        private final Deque<Instant> janela = new ArrayDeque<>();
        private LocalDate dia;
        private int contagemDia;
        private Instant ultimoAcesso = Instant.EPOCH;

        private EstadoUsuario(LocalDate dia) {
            this.dia = dia;
        }
    }
}
