package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.ProjectForAIChatBot;
import dev.franke.felipe.website_backend.dto.SkillForAI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotTools {

    private final ProjectService projectService;
    private final SkillService skillService;

    @Tool(description = "Lista os projetos do Felipe cadastrados na base, com nome, descrição e stack utilizada. "
            + "Use quando a pergunta for sobre os projetos, trabalhos ou tecnologias que ele já aplicou.")
    public List<ProjectForAIChatBot> retrieveProjects() {
        log.info("AI called retrieveProjects()");
        return projectService.getProjectsChatbotVersion();
    }

    @Tool(description = "Lista as habilidades técnicas do Felipe, com nome, categoria e nível de conhecimento. "
            + "Use quando a pergunta for sobre o que ele sabe, domina ou está aprendendo.")
    public List<SkillForAI> retrieveSkills() {
        log.info("AI called retrieveSkills()");
        return skillService.getSkillsChatbotVersion();
    }
}
