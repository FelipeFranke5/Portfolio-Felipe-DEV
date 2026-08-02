package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.ProjetoParaIA;
import dev.franke.felipe.website_backend.dto.SkillParaIA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ferramentas que o modelo de IA pode chamar para responder com os dados reais da API.
 *
 * <p>São apenas de leitura, de propósito: mesmo que alguém consiga manipular o prompt, o
 * máximo que a IA consegue fazer é listar o que já é público em {@code /api/projects} e
 * {@code /api/skills}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotTools {

    private final ProjectService projectService;
    private final SkillService skillService;

    @Tool(description = "Lista os projetos do Felipe cadastrados na base, com nome, descrição e stack utilizada. "
            + "Use quando a pergunta for sobre os projetos, trabalhos ou tecnologias que ele já aplicou.")
    public List<ProjetoParaIA> retrieveProjects() {
        log.info("Tool retrieveProjects acionada pelo modelo de IA");
        return projectService.getProjectsParaChatbot();
    }

    @Tool(description = "Lista as habilidades técnicas do Felipe, com nome, categoria e nível de conhecimento. "
            + "Use quando a pergunta for sobre o que ele sabe, domina ou está aprendendo.")
    public List<SkillParaIA> retrieveSkills() {
        log.info("Tool retrieveSkills acionada pelo modelo de IA");
        return skillService.getSkillsParaChatbot();
    }
}
