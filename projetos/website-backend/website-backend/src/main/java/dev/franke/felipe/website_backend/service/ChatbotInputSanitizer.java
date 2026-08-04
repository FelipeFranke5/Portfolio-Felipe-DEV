package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.exception.ChatbotGeneralException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.text.Normalizer;
import java.util.Set;
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

    private static final int TEXT_MAX_LENGTH = 500;

    /*
        Domínio genérico em vez de uma lista fixa de TLDs: "example.xyz" ou "portal.co.uk"
        passavam batido na versão anterior, que só reconhecia com|net|org|io|dev|br|ai.
        A exclusão de extensões de arquivo/tecnologia comuns (js, ts, py...) evita bloquear
        menções legítimas como "Node.js" num chat sobre um dev back-end.
     */
    private static final String NON_DOMAIN_SUFFIXES =
            "js|ts|jsx|tsx|py|rb|go|cs|cpp|java|html|css|json|xml|yml|yaml|sql|php|kt|swift";

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(https?://|www\\.|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+"
                    + "(?!(?:" + NON_DOMAIN_SUFFIXES + ")\\b)[a-z]{2,24}\\b)"
    );

    public String sanitize(String input) {
        if (input == null) {
            throw new ChatbotGeneralException("A mensagem não pode ser nula!");
        }

        String textFromInput = HtmlUtils.htmlUnescape(input);
        textFromInput = Normalizer.normalize(textFromInput, Normalizer.Form.NFKC).strip();

        if (textFromInput.isBlank()) {
            throw new ChatbotGeneralException("A mensagem não pode ser vazia!");
        }

        if (textFromInput.length() > TEXT_MAX_LENGTH) {
            throw new ChatbotGeneralException(
                    "A mensagem não pode ter mais de " + TEXT_MAX_LENGTH + " caracteres!"
            );
        }

        if (URL_PATTERN.matcher(textFromInput).find()) {
            throw new ChatbotGeneralException("A mensagem não pode conter URL(s) ou domínio(s)!");
        }

        return textFromInput;
    }
}
