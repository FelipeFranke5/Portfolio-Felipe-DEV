package dev.franke.felipe.website_backend.config;

import dev.franke.felipe.website_backend.service.ChatbotTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(ChatbotProperties.class)
public class ChatbotConfiguration {

    /** Pedidos além disso estouram {@code RejectedExecutionException} em vez de enfileirar sem teto. */
    private static final int EXECUTOR_QUEUE_CAPACITY = 50;

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
     *
     * <p>A fila também é limitada, com rejeição explícita: {@code Executors.newFixedThreadPool}
     * usa uma {@code LinkedBlockingQueue} sem teto, e usuários diferentes o suficiente para
     * escapar do {@code ChatbotRateLimiter} (que é por usuário, não global) enfileirariam
     * pedidos indefinidamente, um vetor de exaustão de memória. Com a fila cheia, o pedido
     * extra estoura {@code RejectedExecutionException}, que cai no handler genérico de erro
     * do WebSocket e vira uma mensagem de erro para o usuário, em vez de acumular no heap.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService chatbotExecutor(ChatbotProperties chatbotProperties) {
        AtomicInteger atomiCounter = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, "chatbot-" + atomiCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
                chatbotProperties.maxConcurrentCalls(),
                chatbotProperties.maxConcurrentCalls(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(EXECUTOR_QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
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
