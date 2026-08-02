package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.exception.ChatbotGeneralException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normaliza e valida a mensagem que o usuário envia ao chatbot.
 *
 * <p>A ordem das etapas é a parte importante: o texto é desescapado ANTES de passar pela
 * checagem de URL. A versão anterior fazia o inverso, então um payload como
 * {@code &#104;ttps://...} passava pela validação e só virava URL depois — a checagem não
 * valia nada.
 *
 * <p>Vale registrar o que isto NÃO é: uma blocklist de URL não protege contra prompt
 * injection. A proteção real vem de outro lugar — as tools são apenas de leitura, o
 * {@code max-tokens} limita o tamanho da resposta e o rate limit limita o custo.
 */
@Component
public class ChatbotInputSanitizer {

    private static final int TAMANHO_MAXIMO = 500;

    private static final Pattern URL = Pattern.compile(
            "(?i)(https?://|www\\.|\\b[a-z0-9-]+\\.(com|net|org|io|dev|br|ai)\\b)"
    );

    public String sanitizar(String entrada) {
        if (entrada == null) {
            throw new ChatbotGeneralException("A mensagem não pode ser nula!");
        }

        String texto = HtmlUtils.htmlUnescape(entrada);
        texto = Normalizer.normalize(texto, Normalizer.Form.NFKC).strip();

        if (texto.isBlank()) {
            throw new ChatbotGeneralException("A mensagem não pode ser vazia!");
        }

        if (texto.length() > TAMANHO_MAXIMO) {
            throw new ChatbotGeneralException(
                    "A mensagem não pode ter mais de " + TAMANHO_MAXIMO + " caracteres!"
            );
        }

        if (URL.matcher(texto).find()) {
            throw new ChatbotGeneralException("A mensagem não pode conter URL(s) ou domínio(s)!");
        }

        return texto;
    }
}
