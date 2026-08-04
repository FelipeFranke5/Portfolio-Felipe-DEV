package dev.franke.felipe.website_backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtStompChannelInterceptorTest {

    private static final String USER_ATTRIBUTE = "chatbotAuthentication";

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    private Clock clock;
    private JwtStompChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-03T10:00:00Z"), ZoneOffset.UTC);
        interceptor = new JwtStompChannelInterceptor(jwtDecoder, jwtAuthenticationConverter, clock);
    }

    private Jwt jwtExpiringAt(Instant expiresAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "usuario-123")
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
    }

    private StompHeaderAccessor accessorFor(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private Message<byte[]> messageFor(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private MessageChannel noOpChannel() {
        return (message, timeout) -> true;
    }

    @Test
    @DisplayName("CONNECT without Authorization header should be rejected")
    void connectWithoutTokenShouldBeRejected() {
        Message<byte[]> message = messageFor(accessorFor(StompCommand.CONNECT));

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, noOpChannel()));
    }

    @Test
    @DisplayName("CONNECT with a valid token should authenticate and store the session attribute")
    void connectWithValidTokenShouldAuthenticate() {
        Jwt jwt = jwtExpiringAt(clock.instant().plusSeconds(3600));
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt, List.of()));

        StompHeaderAccessor accessor = accessorFor(StompCommand.CONNECT);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        Message<byte[]> message = messageFor(accessor);

        interceptor.preSend(message, noOpChannel());

        assertNotNull(accessor.getSessionAttributes().get(USER_ATTRIBUTE));
        assertNotNull(accessor.getUser());
    }

    @Test
    @DisplayName("SEND with an expired cached token should be rejected, forcing the client to reconnect")
    void sendWithExpiredCachedTokenShouldBeRejected() {
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwtExpiringAt(clock.instant().minusSeconds(60)), List.of());

        StompHeaderAccessor accessor = accessorFor(StompCommand.SEND);
        accessor.getSessionAttributes().put(USER_ATTRIBUTE, authentication);
        accessor.setDestination("/chatbot/new-message");
        Message<byte[]> message = messageFor(accessor);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, noOpChannel()));
    }

    @Test
    @DisplayName("When token has the same Instant as the expiresAt, MessageDeliveryException should be thrown")
    void sendWithTokenThatHasSameInstantAsExpiresAtTokenShouldRejectAuthentication() {
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwtExpiringAt(clock.instant()), List.of());

        StompHeaderAccessor accessor = accessorFor(StompCommand.SEND);
        accessor.getSessionAttributes().put(USER_ATTRIBUTE, authentication);
        accessor.setDestination("/chatbot/new-message");
        Message<byte[]> message = messageFor(accessor);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, noOpChannel()));
    }

    @Test
    @DisplayName("SEND with a still-valid cached token should be authorized to /chatbot/**")
    void sendWithValidCachedTokenShouldBeAuthorized() {
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwtExpiringAt(clock.instant().plusSeconds(3600)), List.of());

        StompHeaderAccessor accessor = accessorFor(StompCommand.SEND);
        accessor.getSessionAttributes().put(USER_ATTRIBUTE, authentication);
        accessor.setDestination("/chatbot/new-message");
        Message<byte[]> message = messageFor(accessor);

        Message<?> result = interceptor.preSend(message, noOpChannel());

        assertNotNull(result);
        assertNotNull(accessor.getUser());
    }

    @Test
    @DisplayName("SUBSCRIBE outside /user/queue/** should be rejected even with a valid cached token")
    void subscribeOutsideUserQueueShouldBeRejected() {
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwtExpiringAt(clock.instant().plusSeconds(3600)), List.of());

        StompHeaderAccessor accessor = accessorFor(StompCommand.SUBSCRIBE);
        accessor.getSessionAttributes().put(USER_ATTRIBUTE, authentication);
        accessor.setDestination("/queue/chatbot/respostas");
        Message<byte[]> message = messageFor(accessor);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, noOpChannel()));
    }
}
