package dev.franke.felipe.website_backend.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    /*
        Nota mental.
        
        Usar Customizer.withDefaults() no oauth2ResourceServer configura o 
        JwtAuthenticationConverter padrão do Spring Security, que só lê o claim scope/scp 
        do JWT e transforma em authorities SCOPE_xxx. O Keycloak por padrão não coloca 
        as roles em scope e simm em realm_access.roles, um claim customizado.

        Neste caso, precisamos criar um bean do tipo JwtAuthenticationConverter
        para que o Spring possa ler corretamente as roles. Se isso não for
        implementado, gera um erro de 403 mesmo enviando
        um token correto pelo Identity Provider.
    */
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rolesKey = "roles";
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || realmAccess.get(rolesKey) == null) {
                return List.of();
            }
            List<String> roles = (List<String>) realmAccess.get(rolesKey);
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) {
        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(HttpMethod.GET, "/api/projects/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/skills/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
                    /*
                        Handshake do WebSocket/SockJS.

                        Liberado sem método porque o SockJS usa GET e POST (/info, /xhr,
                        /xhr_send, ...). Isso NÃO deixa o chatbot aberto: a autenticação
                        acontece no frame STOMP CONNECT, no JwtStompChannelInterceptor,
                        já que o navegador não envia o header Authorization no upgrade
                        de um WebSocket nativo.
                     */
                    .requestMatchers("/api/websocket", "/api/websocket/**").permitAll()
                    .anyRequest().hasRole("ADMIN")

                );
        return httpSecurity.build();
    }
}
