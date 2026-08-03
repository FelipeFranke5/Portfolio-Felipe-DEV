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

    private static final String USER1 = "usuario-a";
    private static final String USER2 = "usuario-b";

    private static final int MAX_PER_MINUTE = 3;
    private static final int MAX_DAILY_USAGE = 5;
    private static final int MAX_CONCURRENT_CALLS = 3;

    private CustomClockImplementation customClock;
    private ChatbotRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        customClock = new CustomClockImplementation(Instant.parse("2026-08-01T10:00:00Z"));
        rateLimiter = new ChatbotRateLimiter(
            new ChatbotProperties(MAX_PER_MINUTE, MAX_DAILY_USAGE, MAX_CONCURRENT_CALLS), 
            customClock
        );
    }

    @Test
    @DisplayName(
        "When the user exceeds the Max-per-minute limit, calling tryConsume " +
        "should return EXCEEDED_PER_MINUTE_RATE_LIMITING value"
    )
    void userExceedsMaxPerMinuteRateLimitShouldReturnExceededPerMinuteRateLimitingValue() {
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_PER_MINUTE_RATE_LIMITING, rateLimiter.tryConsume(USER1));
    }

    @Test
    @DisplayName(
        "When the user exceeds the Max-per-minute limit, waits some time and " +
        "call again, tryConsume should return USER_ALLOWED value"
    )
    void userExceedsAndWaitShouldFreeSubsequentCallsAfterSomeTime() {
        for (int simulatedAttemptCount = 0; simulatedAttemptCount < MAX_PER_MINUTE; simulatedAttemptCount++) {
            rateLimiter.tryConsume(USER1);
        }

        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_PER_MINUTE_RATE_LIMITING, rateLimiter.tryConsume(USER1));

        customClock.advanceTime(Duration.ofSeconds(61));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
    }

    @Test
    @DisplayName("When the user exceeds the Daily limit, calling tryConsume should return EXCEEDED_DAILY_QUOTA")
    void userExceedsDailyQuotaShouldReturnExceededDailyQuota() {
        // 5 no dia, espacando para nao esbarrar no limite por minuto
        for (int simulatedAttemptCount = 0; simulatedAttemptCount < MAX_DAILY_USAGE; simulatedAttemptCount++) {
            assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
            customClock.advanceTime(Duration.ofSeconds(61));
        }

        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_DAILY_QUOTA, rateLimiter.tryConsume(USER1));

        customClock.advanceTime(Duration.ofDays(1));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));

        customClock.advanceTime(Duration.ofDays(3));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
    }

    @Test
    @DisplayName("When some user exceeds a limit, the other should NOT be affected")
    void whenOneUserExceedsAnyLimitTheOtherShouldNotBeAffectedAndViceVersa() {
        for (int simulatedAttemptCount = 0; simulatedAttemptCount < MAX_PER_MINUTE; simulatedAttemptCount++) {
            rateLimiter.tryConsume(USER1);
        }

        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_PER_MINUTE_RATE_LIMITING, rateLimiter.tryConsume(USER1));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER2));

        // User 1 excede limite diário e User 2 excede limite por minuto
        // Nesse caso, o User 2 pode aguardar e enviar de novo
        customClock.advanceTime(Duration.ofSeconds(120));

        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
        customClock.advanceTime(Duration.ofSeconds(120));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_DAILY_QUOTA, rateLimiter.tryConsume(USER1));

        for (int simulatedAttemptCount = 0; simulatedAttemptCount < MAX_PER_MINUTE + 1; simulatedAttemptCount++) {
            rateLimiter.tryConsume(USER2);
        }

        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_PER_MINUTE_RATE_LIMITING, rateLimiter.tryConsume(USER2));

        // User 1 insistente tenta de novo
        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_DAILY_QUOTA, rateLimiter.tryConsume(USER1));

        // User 2 também
        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_PER_MINUTE_RATE_LIMITING, rateLimiter.tryConsume(USER2));

        // Após esperar, o User 2 é liberado
        customClock.advanceTime(Duration.ofSeconds(120));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER2));
    }

    @Test
    @DisplayName("When the user exceeds any limit, the warning message itself should not consume user's quota")
    void whenUserExceedsLimitWarningMessageShouldNotBeConsumed() {
        for (int simulatedAttemptCount = 0; simulatedAttemptCount < MAX_PER_MINUTE; simulatedAttemptCount++) {
            rateLimiter.tryConsume(USER1);
        }
        // Estoura o limite por minuto varias vezes: nenhuma delas pode contar no dia.
        for (int simulatedAttemptCount = 0; simulatedAttemptCount < 10; simulatedAttemptCount++) {
            assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_PER_MINUTE_RATE_LIMITING, rateLimiter.tryConsume(USER1));
        }

        customClock.advanceTime(Duration.ofSeconds(61));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
        customClock.advanceTime(Duration.ofSeconds(61));
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
    }

    @Test
    @DisplayName("When user is idle, limits are reset")
    void deveEsquecerUsuariosOciosos() {
        for (int simulatedAttemptCount = 0; simulatedAttemptCount < MAX_PER_MINUTE; simulatedAttemptCount++) {
            rateLimiter.tryConsume(USER1);
        }

        assertEquals(ChatbotRateLimiter.AllowUserDecision.EXCEEDED_PER_MINUTE_RATE_LIMITING, rateLimiter.tryConsume(USER1));

        customClock.advanceTime(ChatbotRateLimiter.IDLE_USER_TIME_TO_LIVE.plusMinutes(1));
        rateLimiter.clearIdleUsers();

        // Estado zerado: volta a aceitar as 3 do minuto.
        assertEquals(ChatbotRateLimiter.AllowUserDecision.USER_ALLOWED, rateLimiter.tryConsume(USER1));
    }

    private static final class CustomClockImplementation extends Clock {

        private Instant nowInstant;

        private CustomClockImplementation(Instant nowInstant) {
            this.nowInstant = nowInstant;
        }

        private void advanceTime(Duration duracao) {
            this.nowInstant = this.nowInstant.plus(duracao);
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
            return nowInstant;
        }
    }
}
