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
import org.springframework.stereotype.Component;

import java.security.Principal;
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
 * pegando o handshake) ou fica aberto para qualquer visitante — e a IA é paga.
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

    static final String ATRIBUTO_USUARIO = "chatbotAuthentication";

    private static final String PREFIXO_BEARER = "Bearer ";
    private static final String PREFIXO_DESTINO_APLICACAO = "/chatbot/";
    private static final String PREFIXO_DESTINO_USUARIO = "/user/queue/";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        return switch (accessor.getCommand()) {
            case CONNECT, STOMP -> autenticar(message, accessor);
            case SEND -> autorizarEnvio(message, accessor);
            case SUBSCRIBE -> autorizarAssinatura(message, accessor);
            default -> restaurarUsuario(message, accessor);
        };
    }

    private Message<?> autenticar(Message<?> message, StompHeaderAccessor accessor) {
        String autorizacao = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (autorizacao == null || !autorizacao.startsWith(PREFIXO_BEARER)) {
            log.warn("CONNECT recusado: sessao {} nao enviou token", accessor.getSessionId());
            throw new MessageDeliveryException(message, "Token ausente no CONNECT");
        }

        Authentication autenticacao;
        try {
            Jwt jwt = jwtDecoder.decode(autorizacao.substring(PREFIXO_BEARER.length()));
            autenticacao = jwtAuthenticationConverter.convert(jwt);
        } catch (JwtException tokenInvalidoException) {
            log.warn("CONNECT recusado: token invalido na sessao {}", accessor.getSessionId());
            throw new MessageDeliveryException(message, "Token invalido");
        }

        if (autenticacao == null) {
            throw new MessageDeliveryException(message, "Token invalido");
        }

        accessor.setUser(autenticacao);
        guardarUsuarioNaSessao(accessor, autenticacao);
        log.debug("Sessao STOMP {} autenticada como {}", accessor.getSessionId(), autenticacao.getName());
        return message;
    }

    private Message<?> autorizarEnvio(Message<?> message, StompHeaderAccessor accessor) {
        restaurarUsuario(message, accessor);
        exigirUsuario(message, accessor);

        String destino = accessor.getDestination();
        if (destino == null || !destino.startsWith(PREFIXO_DESTINO_APLICACAO)) {
            log.warn("SEND recusado para o destino '{}'", destino);
            throw new MessageDeliveryException(message, "Destino nao permitido");
        }
        return message;
    }

    private Message<?> autorizarAssinatura(Message<?> message, StompHeaderAccessor accessor) {
        restaurarUsuario(message, accessor);
        exigirUsuario(message, accessor);

        // Só filas individuais. Sem isso, um usuário poderia assinar a fila crua e ler
        // as respostas destinadas a outra pessoa.
        String destino = accessor.getDestination();
        if (destino == null || !destino.startsWith(PREFIXO_DESTINO_USUARIO)) {
            log.warn("SUBSCRIBE recusado para o destino '{}'", destino);
            throw new MessageDeliveryException(message, "Destino nao permitido");
        }
        return message;
    }

    /**
     * O Spring repõe o {@code Principal} da sessão nos frames seguintes ao CONNECT, mas isso
     * depende do callback interno do {@code StompSubProtocolHandler}. Reler dos atributos da
     * sessão garante o comportamento independentemente desse detalhe de implementação.
     */
    private Message<?> restaurarUsuario(Message<?> message, StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return message;
        }
        Map<String, Object> atributos = accessor.getSessionAttributes();
        if (atributos != null && atributos.get(ATRIBUTO_USUARIO) instanceof Principal usuario) {
            accessor.setUser(usuario);
        }
        return message;
    }

    private void exigirUsuario(Message<?> message, StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessageDeliveryException(message, "Sessao nao autenticada");
        }
    }

    private void guardarUsuarioNaSessao(StompHeaderAccessor accessor, Authentication autenticacao) {
        Map<String, Object> atributos = accessor.getSessionAttributes();
        if (atributos != null) {
            atributos.put(ATRIBUTO_USUARIO, autenticacao);
        }
    }
}
