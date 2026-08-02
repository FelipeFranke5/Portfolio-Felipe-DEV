package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.exception.ChatbotGeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatbotInputSanitizerTest {

    private final ChatbotInputSanitizer sanitizer = new ChatbotInputSanitizer();

    @Test
    @DisplayName("Entidade HTML nao pode furar a checagem de URL")
    void naoDeveDeixarEntidadeHtmlFurarAChecagemDeUrl() {
        /*
            Regressao: a versao anterior rodava a blocklist ANTES do htmlUnescape, entao
            este payload passava pela validacao e so virava URL depois.
         */
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar("&#104;ttps://site-malicioso.com"));
    }

    @Test
    @DisplayName("Deve recusar URL escrita normalmente")
    void deveRecusarUrlEscritaNormalmente() {
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar("Veja https://exemplo.com"));
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar("Acesse www.exemplo.org"));
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar("Confira exemplo.dev"));
    }

    @Test
    @DisplayName("Deve recusar dominio independente de caixa")
    void deveRecusarDominioIndependenteDeCaixa() {
        // A versao anterior comparava com case-sensitive, entao ".COM" escapava.
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar("Veja EXEMPLO.COM agora"));
    }

    @Test
    @DisplayName("Deve recusar mensagem nula, vazia ou so com espacos")
    void deveRecusarMensagemNulaOuVazia() {
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar(null));
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar(""));
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar("     "));
    }

    @Test
    @DisplayName("Deve recusar mensagem acima do tamanho maximo")
    void deveRecusarMensagemMuitoLonga() {
        String longa = "a".repeat(501);
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitizar(longa));
    }

    @Test
    @DisplayName("Deve aceitar pergunta valida e remover espacos das pontas")
    void deveAceitarPerguntaValida() {
        assertEquals("Quais projetos o Felipe tem?", sanitizer.sanitizar("  Quais projetos o Felipe tem?  "));
    }

    @Test
    @DisplayName("Deve aceitar mensagem que nao seja pergunta")
    void deveAceitarMensagemQueNaoSejaPergunta() {
        /*
            A regra de "tem que terminar com ?" saiu do codigo: "Me fale dos projetos" e
            legitimo, e o proprio prompt do sistema ja instrui o modelo a recusar o que
            estiver fora de escopo.
         */
        assertEquals("Me fale dos projetos", sanitizer.sanitizar("Me fale dos projetos"));
    }

    @Test
    @DisplayName("Deve devolver o texto desescapado")
    void deveDevolverOTextoDesescapado() {
        assertEquals("Ele usa <Java> & Spring?", sanitizer.sanitizar("Ele usa &lt;Java&gt; &amp; Spring?"));
    }
}
