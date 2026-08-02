package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.ProjectReadOnly;
import dev.franke.felipe.website_backend.dto.SkillDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatbotTools {

    private final ProjectService projectService;
    private final SkillService skillService;

    ChatbotTools(ProjectService projectService, SkillService skillService) {
        this.projectService = projectService;
        this.skillService = skillService;
    }

    @Tool(description = "Get the projects that are registered in the database")
    List<ProjectReadOnly> retrieveProjects() {
        return projectService.getProjects();
    }

    @Tool(description = "Get the skills that are registered in the database")
    List<SkillDTO> retrieveSkills() {
        return skillService.getSkills();
    }
}
