package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.ChatbotInput;
import dev.franke.felipe.website_backend.dto.ChatbotOutput;
import dev.franke.felipe.website_backend.exception.ChatbotGeneralException;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Log4j2
public class ChatbotService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Value("classpath:prompt-sistema.txt")
    private Resource aiSystemPromptResource;

    private final ChatClient chatClient;
    private final ProjectService projectService;
    private final SkillService skillService;

    public ChatbotService(
            ChatClient.Builder chatClientBuilder,
            SimpMessagingTemplate simpMessagingTemplate,
            ProjectService projectService,
            SkillService skillService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.projectService = projectService;
        this.skillService = skillService;
    }

    public void sendMessages(ChatbotInput input) {
        log.info("Sending messages in sequence (through async tasks)");
        ChatbotOutput ownMessageOutput = new ChatbotOutput(input.name(), input.message(), LocalDateTime.now());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var ownMessageTask = executor.submit(() -> sendMessage(input, ownMessageOutput));
            var automaticResponseTask = executor.submit(() -> sendMessage(input, true));
            ownMessageTask.get();
            log.info("Sent own message");
            automaticResponseTask.get();
            log.info("Sent automatic message");
            log.info("Submitting task to retrieve AI Response..");
            executor.submit(() -> sendMessage(input, false));
        } catch (Exception exception) {
            ChatbotOutput systemErrorOutput = new ChatbotOutput(
                "sistema", 
                "Erro na execução do envio de uma das mensagens!", 
                LocalDateTime.now()
            );
            sendMessage(input, systemErrorOutput);
        }
    }

    public String validateAndReturnMessage(String input, String inputName) {
        if (input == null) throw  new ChatbotGeneralException(inputName + " não pode ser nulo(a)!");
        if (input.contains("https://")) throw new ChatbotGeneralException(inputName + " não pode ter URL(s)");
        if (input.contains("http://")) throw new ChatbotGeneralException(inputName + " não pode ter URL(s)");
        if (input.contains(".com")) throw new ChatbotGeneralException(inputName + " não pode ter URL(s) ou domínios(s)");
        if (input.contains(".net")) throw new ChatbotGeneralException(inputName + " não pode ter URL(s) ou domínio(s)");

        if ("mensagem".equals(inputName) && !input.endsWith("?")) {
            throw new ChatbotGeneralException(inputName + " deve terminar com '?'");
        }

        return getUnescaped(input);
    }

    private void sendMessage(ChatbotInput chatbotInput, ChatbotOutput chatbotOutput) {
        simpMessagingTemplate.convertAndSendToUser(
            chatbotInput.name(),
            "/queue/messages-sent",
            chatbotOutput
        );
    }

    private void sendMessage(ChatbotInput chatbotInput, boolean automaticResponse) {
        simpMessagingTemplate.convertAndSendToUser(
            chatbotInput.name(),
            "/queue/response-to-user",
            automaticResponse ? getAutomaticResponse() : getAIResponse(chatbotInput)
        );
    }

    private ChatbotOutput getAutomaticResponse() {
        return new ChatbotOutput("sistema", "Mensagem em processamento..", LocalDateTime.now());
    }

    private ChatbotOutput getAIResponse(ChatbotInput chatbotInput) {
        log.info("Getting AI response");
        try {
            String aiResponse = chatClient.prompt(chatbotInput.message())
                    .system(getSystemPrompt())
                    .tools(new ChatbotTools(projectService, skillService))
                    .call()
                    .content();
            return new ChatbotOutput("bot", aiResponse, LocalDateTime.now());
        } catch (Exception aiResponseException) {
            log.error("Unable to obtain AI response", aiResponseException);
            return new ChatbotOutput("bot", "Erro ao obter resposta!", LocalDateTime.now());
        }
    }

    private String getUnescaped(String message) {
        return HtmlUtils.htmlUnescape(message);
    }

    private String getSystemPrompt() throws IOException {
        try (Reader reader = new InputStreamReader(aiSystemPromptResource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}
