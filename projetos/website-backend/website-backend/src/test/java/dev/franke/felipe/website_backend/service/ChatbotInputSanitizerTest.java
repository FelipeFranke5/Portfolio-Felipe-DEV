package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.exception.ChatbotGeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatbotInputSanitizerTest {

    private final ChatbotInputSanitizer sanitizer = new ChatbotInputSanitizer();

    @Test
    @DisplayName("HTML Entity should NOT bypass URL validation")
    void htmlEntityShouldNotBypassURLValidation() {
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize("&#104;ttps://site-malicioso.com"));
    }

    @Test
    @DisplayName("When a URL is sent, ChatbotGeneralException should be thrown")
    void inputWithURLShouldThrow() {
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize("Veja https://exemplo.com"));
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize("Acesse www.exemplo.org"));
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize("Confira exemplo.dev"));
    }

    @Test
    @DisplayName("When domain is sent, ChatbotGeneralException should be thrown")
    void inputWithDomainShouldThrow() {
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize("Veja EXEMPLO.COM agora"));
    }

    @Test
    @DisplayName("When domain has a TLD outside the old fixed list, ChatbotGeneralException should be thrown")
    void inputWithArbitraryTldDomainShouldThrow() {
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize("Confira example.xyz"));
    }

    @Test
    @DisplayName("When domain has a multi-label hostname, ChatbotGeneralException should be thrown")
    void inputWithMultiLabelHostnameShouldThrow() {
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize("Visite portal.co.uk"));
    }

    @Test
    @DisplayName("When input mentions a tech name that looks like a domain, it should NOT be blocked")
    void inputWithTechNameShouldNotBeBlocked() {
        assertEquals("Você usa Node.js?", sanitizer.sanitize("Você usa Node.js?"));
    }

    @Test
    @DisplayName("When input is null or blank, ChatbotGeneralException should be thrown")
    void nullOrBlankInputShouldThrow() {
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize(null));
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize(""));
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize("     "));
    }

    @Test
    @DisplayName("When input is too long, ChatbotGeneralException should be thrown")
    void tooLongInputShouldThrow() {
        String longString = "a".repeat(501);
        assertThrows(ChatbotGeneralException.class, () -> sanitizer.sanitize(longString));
    }

    @Test
    @DisplayName("When input is a valid question, the sanitizer should remove white spaces")
    void validInputWithWhiteSpaceShouldRemoveWhiteSpace() {
        assertEquals("Quais projetos o Felipe tem?", sanitizer.sanitize("  Quais projetos o Felipe tem?  "));
        assertEquals("Quais projetos o Felipe tem?", sanitizer.sanitize("Quais projetos o Felipe tem? "));
        assertEquals("Quais projetos o Felipe tem?", sanitizer.sanitize(" Quais projetos o Felipe tem?"));
        assertEquals("Quais projetos o Felipe tem?", sanitizer.sanitize("Quais projetos o Felipe tem?"));
    }

    @Test
    @DisplayName("When input is valid but contains escaped characters, the sanitizer should return unescaped text")
    void validInputWithEscapedTextShouldReturnUnescapedText() {
        assertEquals("Ele usa <Java> & Spring?", sanitizer.sanitize("Ele usa &lt;Java&gt; &amp; Spring?"));
    }
}
