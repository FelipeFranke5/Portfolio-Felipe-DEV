package dev.franke.felipe.website_backend.config;

import dev.franke.felipe.website_backend.service.ChatbotTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(ChatbotProperties.class)
public class ChatbotConfiguration {

    /**
     * O ChatClient é montado uma única vez, no startup: o prompt do sistema é lido do
     * classpath aqui (e não a cada mensagem) e as tools vêm do bean, em vez de uma
     * instância nova por requisição.
     *
     * <p>Expor o ChatClient como bean também é o que torna os testes honestos — um
     * {@code @MockitoBean ChatClient} passa a substituir de verdade o colaborador, então
     * nenhum teste alcança a Anthropic.
     */
    @Bean
    public ChatClient chatbotChatClient(
            ChatClient.Builder chatClientBuilder,
            ChatbotTools chatbotTools,
            @Value("classpath:prompt-sistema.txt") Resource systemPromptResource
    ) {
        if (!systemPromptResource.exists()) {
            throw new IllegalStateException(
                    "prompt-sistema.txt not found in the classpath. "
                            + "The file should live in src/main/resources."
            );
        }
        return chatClientBuilder
                .defaultSystem(systemPromptResource)
                .defaultTools(chatbotTools)
                .build();
    }

    /**
     * Teto de chamadas pagas simultâneas. Um executor sem limite deixaria N usuários
     * dispararem N requisições à Anthropic ao mesmo tempo.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService chatbotExecutor(ChatbotProperties chatbotProperties) {
        AtomicInteger atomiCounter = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, "chatbot-" + atomiCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(chatbotProperties.maxConcurrentCalls(), threadFactory);
    }

    /**
     * Injetado no {@code ChatbotRateLimiter} para que a janela deslizante e o teto diário
     * possam ser testados sem depender do relógio real.
     */
    @Bean
    public Clock chatbotClock() {
        return Clock.systemDefaultZone();
    }
}
