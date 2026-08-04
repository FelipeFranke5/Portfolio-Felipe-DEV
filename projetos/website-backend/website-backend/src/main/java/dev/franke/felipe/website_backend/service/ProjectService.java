package dev.franke.felipe.website_backend.service;

import dev.franke.felipe.website_backend.dto.ProjectReadOnly;
import dev.franke.felipe.website_backend.dto.ProjectReadOnlyDetailed;
import dev.franke.felipe.website_backend.dto.ProjectRequest;
import dev.franke.felipe.website_backend.dto.ProjectForAIChatBot;
import dev.franke.felipe.website_backend.exception.ProjectException;
import dev.franke.felipe.website_backend.exception.ProjectNotFoundException;
import dev.franke.felipe.website_backend.model.Project;
import dev.franke.felipe.website_backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {

    public static final int DESCRIPTION_MAX_LENGTH = 50;
    public static final int DESCRIPTION_MAX_LENGTH_CHATBOT = 400;

    private final ProjectRepository repository;

    public List<ProjectReadOnly> getProjects() {
        log.info("Starting the getProjects method");
        List<Project> projects = repository.findAll();
        log.info("List of projects retrieved!");
        log.trace("Full List: {}", projects);
        List<ProjectReadOnly> results = new ArrayList<>();
        log.debug("Going through each project. Total size: {}", projects.size());
        projects.forEach(project -> {
            log.trace("Project: {}", project.getName());
            log.trace(
                    "Description length: {}",
                    project.getDescription() != null ? project.getDescription().length() : 0
            );
            String usedDescription = project.getDescription();
            if (usedDescription != null && usedDescription.length() > DESCRIPTION_MAX_LENGTH) {
                usedDescription = usedDescription.substring(0, DESCRIPTION_MAX_LENGTH) + "...";
                log.trace("Changed the value of Description due to String length");
                log.trace("New description length: {}", usedDescription.length());
            }
            ProjectReadOnly projectReadOnly = new ProjectReadOnly(
                    project.getId(),
                    project.getName(),
                    usedDescription,
                    project.getStack()
            );
            results.add(projectReadOnly);
        });
        log.info("Exiting the getProjects method and returning results");
        return results;
    }

    /**
     * Projetos no formato consumido pelas tool calls do chatbot.
     *
     * <p>Existe separado do {@link #getProjects()} porque aquele trunca a descrição em
     * {@link #DESCRIPTION_MAX_LENGTH} caracteres — contrato de {@code GET /api/projects},
     * que não muda. A IA precisa da descrição inteira para responder algo útil, mas com um
     * teto próprio: cada tool call devolve o payload completo de volta ao modelo, então
     * descrição sem limite vira custo de token.
     */
    public List<ProjectForAIChatBot> getProjectsChatbotVersion() {
        log.info("Grouping prokects in order to send to AI");
        return repository.findAll().stream()
                .map(project -> new ProjectForAIChatBot(
                        project.getName(),
                        chatbotTruncate(project.getDescription()),
                        project.getStack()
                ))
                .toList();
    }

    private String chatbotTruncate(String description) {
        if (description == null || description.length() <= DESCRIPTION_MAX_LENGTH_CHATBOT) {
            return description;
        }

        return description.substring(0, DESCRIPTION_MAX_LENGTH_CHATBOT) + "...";
    }

    public ProjectReadOnlyDetailed getProjectDTO(Project project) {
        return new ProjectReadOnlyDetailed(
            project.getId(), 
            project.getName(), 
            project.getDescription(), 
            project.getStack(), 
            project.getGithubURL(), 
            project.getDemoURL(), 
            project.isFeatured(), 
            project.getCreatedAt()
        );
    }

    public Project getProject(String id) {
        log.info("Entered method to retrieve a project using id {}", id);
        UUID idInUUIDFormat = null;
        try {
            idInUUIDFormat = UUID.fromString(id);
        } catch (Exception parseIdException) {
            throw new ProjectException("Unparsable ID '" + id + "'. Should be in UUID format");
        }
        Project project = repository
                .findById(idInUUIDFormat)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID '" + id + "'"));
        log.info("Returning from method to retrieve a project using id {}. Object {}", id, project);
        return project;
    }

    public void saveProject(ProjectRequest request) {
        log.info("Entered method to Save a new Project with request: {}", request);
        if (request == null) {
            throw new ProjectException("The Project Request is required");
        }
        Optional<Project> existingProject = repository.findByName(request.name());
        if (existingProject.isPresent()) {
            log.warn("Project with name '{}' already exists", request.name());
            throw new ProjectException("Project with name '" + request.name() + "' already exists");
        }
        Project newProject = new Project();
        log.debug("Calling performSaveBasedOnRequest");
        performSaveBasedOnRequest(newProject, request);
        log.debug("Called performSaveBasedOnRequest and object saved!");
        log.info("Exiting saveProject");
    }

    public void updateProject(Project project, ProjectRequest request) {
        log.info("Entered the updateProject method");
        if (project == null || request == null) {
            throw new ProjectException("The project / request is required");
        }
        performSaveBasedOnRequest(project, request);
        log.info("Exiting the updateProject method");
    }

    public void deleteProject(Project project) {
        log.info("Entered deleteProject");
        if (project == null) {
            log.error("Value of the project is NULL. Throwing");
            throw new ProjectException("Project must not be null");
        }
        repository.delete(project);
        log.info("Exiting deleteProject method");
    }

    private void performSaveBasedOnRequest(Project project, ProjectRequest request) {
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStack(request.stack());
        project.setGithubURL(request.githubURL());
        project.setDemoURL(request.demoURL());
        project.setFeatured(request.featured());
        repository.save(project);
    }


}
