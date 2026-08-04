package dev.franke.felipe.website_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Autenticação e autorização das mensagens STOMP do chatbot.
 *
 * <p>Nota mental.
 *
 * <p>O handshake HTTP de {@code /api/websocket} é liberado no {@link SecurityConfig}, porque o
 * navegador NÃO consegue enviar o header {@code Authorization} no upgrade de um WebSocket nativo.
 * A autenticação de verdade acontece aqui, no frame STOMP {@code CONNECT}, onde o cliente envia
 * o token do Keycloak como header nativo. Sem isso, o chat ou fica inacessível (regra ADMIN
 * pegando o handshake) ou fica aberto para qualquer visitante.
 * 
 * <p>Também NÃO usamos {@code @EnableWebSocketSecurity}: em Spring Security 7 ele acopla um
 * {@code CsrfChannelInterceptor} ao CONNECT, que exige um token CSRF guardado na sessão HTTP.
 * Como a API é stateless e o CSRF está desabilitado no {@link SecurityConfig}, esse atributo
 * nunca existe e qualquer CONNECT falharia. O vetor que o CSRF de mensagens protege
 * (cross-site WebSocket hijacking) depende da credencial viajar em cookie; aqui ela é um header
 * Bearer explícito, que um site malicioso não tem como obter. A autorização de destinos fica
 * neste mesmo interceptor, logo abaixo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    static final String USER_ATTRIBUTE = "chatbotAuthentication";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHATBOT_PATH_PREFIX = "/chatbot/";
    private static final String USER_QUEUE_PREFIX = "/user/queue/";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final Clock clock;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        return switch (accessor.getCommand()) {
            case CONNECT, STOMP -> authenticate(message, accessor);
            case SEND -> authorizeMessage(message, accessor);
            case SUBSCRIBE -> authorizeSubscription(message, accessor);
            default -> reloadUser(message, accessor);
        };
    }

    private Message<?> authenticate(Message<?> message, StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            log.warn("CONNECT refused: sessao {} did NOT send token", accessor.getSessionId());
            throw new MessageDeliveryException(message, "Token ausente no CONNECT");
        }

        Authentication authentication;

        try {
            Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
            authentication = jwtAuthenticationConverter.convert(jwt);
        } catch (JwtException invalidTokenException) {
            log.warn("CONNECT refused: invalid token. Session ID: {}", accessor.getSessionId());
            throw new MessageDeliveryException(message, "Token invalido");
        }

        if (authentication == null) {
            throw new MessageDeliveryException(message, "Token invalido");
        }

        accessor.setUser(authentication);
        saveUserSession(accessor, authentication);
        log.debug("STOMP Session {} - Authenticated as {}", accessor.getSessionId(), authentication.getName());
        return message;
    }

    private Message<?> authorizeMessage(Message<?> message, StompHeaderAccessor accessor) {
        reloadUser(message, accessor);
        requireUserAuth(message, accessor);

        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(CHATBOT_PATH_PREFIX)) {
            log.warn("SEND refused for destination '{}'", destination);
            throw new MessageDeliveryException(message, "Destino nao permitido");
        }

        return message;
    }

    private Message<?> authorizeSubscription(Message<?> message, StompHeaderAccessor accessor) {
        reloadUser(message, accessor);
        requireUserAuth(message, accessor);

        // Só filas individuais. Sem isso, um usuário poderia assinar a fila crua e ler
        // as respostas destinadas a outra pessoa.
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(USER_QUEUE_PREFIX)) {
            log.warn("SUBSCRIBE refused for destination '{}'", destination);
            throw new MessageDeliveryException(message, "Destino nao permitido");
        }

        return message;
    }

    /**
     * O Spring repõe o {@code Principal} da sessão nos frames seguintes ao CONNECT, mas isso
     * depende do callback interno do {@code StompSubProtocolHandler}. Reler dos atributos da
     * sessão garante o comportamento independentemente desse detalhe de implementação.
     *
     * <p>Além de repor, revalida a expiração do JWT em TODO frame pós-CONNECT: sem isso, um
     * token que expira no meio de uma sessão STOMP longa continuava sendo aceito até o
     * cliente desconectar, já que a autenticação original ficava só guardada na sessão.
     */
    private Message<?> reloadUser(Message<?> message, StompHeaderAccessor accessor) {
        Authentication authentication = resolveAuthentication(accessor);

        if (authentication == null) {
            return message;
        }

        if (isExpired(authentication)) {
            log.warn("Frame refused: session {} token expired", accessor.getSessionId());
            throw new MessageDeliveryException(message, "Token expirado");
        }

        accessor.setUser(authentication);
        return message;
    }

    private Authentication resolveAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication) {
            return authentication;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes != null && sessionAttributes.get(USER_ATTRIBUTE) instanceof Authentication authentication) {
            return authentication;
        }

        return null;
    }

    private boolean isExpired(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return false;
        }

        Instant expiresAt = jwtAuthenticationToken.getToken().getExpiresAt();
        return expiresAt != null && expiresAt.isBefore(clock.instant());
    }

    private void requireUserAuth(Message<?> message, StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessageDeliveryException(message, "Sessao nao autenticada");
        }
    }

    private void saveUserSession(StompHeaderAccessor accessor, Authentication autenticacao) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes != null) {
            sessionAttributes.put(USER_ATTRIBUTE, autenticacao);
        }
    }
}
