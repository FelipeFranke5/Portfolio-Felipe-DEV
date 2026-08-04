package dev.franke.felipe.website_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    /*
        A mensagem do usuário já é limitada na validação do DTO. 8 KB de frame STOMP é
        folga suficiente para os headers e corta payloads abusivos antes de qualquer
        processamento.
     */
    private static final int MESSAGE_SIZE_LIMIT = 8 * 1024;
    private static final int SEND_BUFFER_LIMIT = 64 * 1024;
    private static final int SEND_TIME_LIMIT = 15 * 1000;
    private static final int FIRST_MESSAGE_TIME_LIMIT = 30 * 1000;

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    @Value("${FRONT_END_URL:http://localhost:4200}")
    private String frontEndURL;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/chatbot");
        // Só /queue: o chat é individual, não há nada para transmitir em /topic.
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/websocket")
                .setAllowedOriginPatterns(frontEndURL, "http://localhost")
                .withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(MESSAGE_SIZE_LIMIT);
        registration.setSendBufferSizeLimit(SEND_BUFFER_LIMIT);
        registration.setSendTimeLimit(SEND_TIME_LIMIT);
        // Fecha quem abre a conexão e nunca envia o CONNECT.
        registration.setTimeToFirstMessage(FIRST_MESSAGE_TIME_LIMIT);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}
