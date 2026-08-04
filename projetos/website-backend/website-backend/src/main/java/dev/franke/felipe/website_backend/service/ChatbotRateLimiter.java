package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.config.ChatbotProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
 * <p>O userState vive só nesta instância. Com uma única réplica na VPS isso basta; se um day
 * houver escala horizontal, o limite precisa migrar para um store compartilhado.
 */
@Service
@Slf4j
public class ChatbotRateLimiter {

    static final Duration DURATION_WINDOW = Duration.ofMinutes(1);
    static final Duration IDLE_USER_TIME_TO_LIVE = Duration.ofHours(6);

    private final ChatbotProperties chatbotProperties;
    private final Clock clock;
    private final Map<String, UserState> perUserControl = new ConcurrentHashMap<>();

    public ChatbotRateLimiter(ChatbotProperties chatbotProperties, Clock clock) {
        this.chatbotProperties = chatbotProperties;
        this.clock = clock;
    }

    /**
     * Registra uma pergunta do usuário e devolve a decisão. Os contadores só são
     * incrementados quando a decisionMessage é permitida.
     */
    public AllowUserDecision tryConsume(String user) {
        Instant nowInstant = clock.instant();
        LocalDate today = LocalDate.ofInstant(nowInstant, clock.getZone());

        UserState userState = perUserControl.computeIfAbsent(user, key -> new UserState(today, nowInstant));

        synchronized (userState) {
            userState.lastAccess = nowInstant;

            if (!today.equals(userState.day)) {
                userState.day = today;
                userState.dailyCount = 0;
            }

            Instant startWindows = nowInstant.minus(DURATION_WINDOW);

            while (!userState.instantWindows.isEmpty() && userState.instantWindows.peekFirst().isBefore(startWindows)) {
                userState.instantWindows.pollFirst();
            }

            if (userState.instantWindows.size() >= chatbotProperties.maxPerMinute()) {
                log.info("User {} exceeded the chatbot's per-minute limit", user);
                return AllowUserDecision.EXCEEDED_PER_MINUTE_RATE_LIMITING;
            }

            if (userState.dailyCount >= chatbotProperties.maxPerUserPerDay()) {
                log.info("User {} exceeded the chatbot's daily limit.", user);
                return AllowUserDecision.EXCEEDED_DAILY_QUOTA;
            }

            userState.instantWindows.addLast(nowInstant);
            userState.dailyCount++;
            return AllowUserDecision.USER_ALLOWED;
        }
    }

    /**
     * Sem esta limpeza o mapa cresceria indefinidamente conforme novos usuários aparecem,
     * o que é um vetor de exaustão de memória.
     */
    @Scheduled(fixedRate = 3_600_000)
    public void clearIdleUsers() {
        Instant cutoff = clock.instant().minus(IDLE_USER_TIME_TO_LIVE);

        perUserControl.entrySet().removeIf(entry -> {
            UserState userState = entry.getValue();
            synchronized (userState) {
                return userState.lastAccess.isBefore(cutoff);
            }
        });
    }

    public enum AllowUserDecision {

        USER_ALLOWED(null),
        EXCEEDED_PER_MINUTE_RATE_LIMITING("Muitas perguntas seguidas. Aguarde um instante antes de tentar novamente."),
        EXCEEDED_DAILY_QUOTA("Você atingiu o limite diário de perguntas ao assistente. Tente novamente amanhã.");

        private final String decisionMessage;

        AllowUserDecision(String decisionMessage) {
            this.decisionMessage = decisionMessage;
        }

        public String decisionMessage() {
            return decisionMessage;
        }

        public boolean isAllowed() {
            return this == USER_ALLOWED;
        }
    }

    private static final class UserState {

        private final Deque<Instant> instantWindows = new ArrayDeque<>();
        private LocalDate day;
        private int dailyCount;
        private Instant lastAccess;

        private UserState(LocalDate day, Instant lastAccess) {
            this.day = day;
            this.lastAccess = lastAccess;
        }
    }
}
