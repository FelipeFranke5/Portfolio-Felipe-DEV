package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.dto.ProjectReadOnly;
import dev.franke.felipe.website_backend.dto.ProjectReadOnlyDetailed;
import dev.franke.felipe.website_backend.dto.ProjectRequest;
import dev.franke.felipe.website_backend.dto.UnprocessableEntityResponse;
import dev.franke.felipe.website_backend.model.Project;
import dev.franke.felipe.website_backend.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.junit.jupiter.api.DisplayName;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class ProjectControllerIntegrationTest {

    private static final int DESCRIPTION_MAX_LENGTH = 50;

    private static final String VALIDATION_ERROR_MESSAGE =
            "The request has failed due to incorrect information in the payload provided.";

    private static final String PROJECT_EXCEPTION_ERROR_MESSAGE =
            "The request has failed due to incorrect use of PROJECT endpoint(s). Please " +
            "review the details in the response body to correct your request.";

    private static final String PROJECT_NOT_FOUND_EXCEPTION_ERROR_MESSAGE =
            "Unable to find the project you attempted to retrieve. Returning 404";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.0");

    @Test
    @DisplayName("Assert that the PSQL container runs")
    void loads() {
        assertTrue(postgreSQLContainer.isCreated());
        assertTrue(postgreSQLContainer.isRunning());
    }

    @Test
    @DisplayName("Assert that the DELETE method returns 204 when the delete is successful")
    void deleteProject() throws Exception {
        repository.deleteAll();
        Project project = new Project();
        project.setName("Project 1");
        project.setDescription("Some Description");
        project.setStack(List.of("Java"));
        repository.save(project);
        String deleteURL = URI.create("/api/projects/" + project.getId()).toString();
        mockMvc
                .perform(delete(deleteURL)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isNoContent());
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the PUT method returns 204 when the update is successful")
    void updateProject() throws Exception {
        repository.deleteAll();
        Project project = new Project();
        project.setName("Project 1");
        project.setDescription("Some Description");
        project.setStack(List.of("Java"));
        project = repository.save(project);
        ProjectRequest requestBody = new ProjectRequest(
                "Project 1",
                "Updated Description of Project 1",
                List.of("Java", "Python"),
                null,
                null,
                false
        );
        String updateURL = URI.create("/api/projects/" + project.getId()).toString();
        mockMvc
                .perform(put(updateURL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNoContent());
        List<Project> projects = repository.findAll();
        assertEquals(1, projects.size());
        assertEquals(requestBody.name(), projects.getFirst().getName());
        assertEquals(requestBody.description(), projects.getFirst().getDescription());
    }

    @Test
    @DisplayName("Assert that the POST method returns 400 when the project with that name already exists")
    void newProjectAlreadyExists() throws Exception {
        repository.deleteAll();
        Project project = new Project();
        project.setName("Project 1");
        project.setDescription("Some Description");
        project.setStack(List.of("Java"));
        repository.save(project);
        ProjectRequest requestBody = new ProjectRequest(
                "Project 1",
                "Project with an already existing project name",
                List.of("Java"),
                null,
                null,
                false
        );
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", PROJECT_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Project with name '" + requestBody.name() + "' already exists");
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertEquals(1, projects.size());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Demo URL is not a valid URL")
    void newProjectWithInvalidDemoURL() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Project 1",
                "Project with an invalid Demo URL",
                List.of("Java"),
                null,
                "Some Random Value",
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("demoURL", List.of("Incorrect URL format for the demo URL"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Github URL is not a valid GitHub URL")
    void newProjectWithGithubURLThatIsActuallyNotAGithubURL() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Project 1",
                "Project with an invalid GitHub URL",
                List.of("Java"),
                "https://www.google.com.br",
                null,
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("githubURL", List.of("The github URL must start with https://github.com/"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Github URL is not a valid URL")
    void newProjectWithInvalidGitHubURL() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Project 1",
                "Project with an invalid GitHub URL",
                List.of("Java"),
                "Some Random Value",
                null,
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("githubURL", List.of("The github URL must start with https://github.com/", "Incorrect URL format for the github URL"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Description has less than 5 characters")
    void newProjectWithDescriptionTooShort() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Project 1",
                "a",
                List.of("Java"),
                null,
                null,
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("description", List.of("The project description should have between 5 and 500 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Description has more than 100 characters")
    void newProjectWithDescriptionTooLong() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Project 1",
                "a".repeat(600),
                List.of("Java"),
                null,
                null,
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("description", List.of("The project description should have between 5 and 500 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Name has less than 5 characters")
    void newProjectWithNameTooShort() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "a",
                "Project where the name is too short",
                List.of("Java"),
                null,
                null,
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("The project name should have between 5 and 100 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Name has more than 100 characters")
    void newProjectWithNameTooLong() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "a".repeat(200),
                "Project where the name is too long",
                List.of("Java"),
                null,
                null,
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("The project name should have between 5 and 100 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Stack field is empty")
    void newProjectEmptyStack() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Empty Stack",
                "Project where the stack is not filled in",
                List.of(),
                null,
                null,
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("stack", List.of("The stack is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the payload contains multiple invalid fields")
    void newProjectIncorrectPayloadWithMoreThanOneIncorrectFields() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                null,
                null,
                null,
                null,
                null,
                false
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent());
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the payload does not meet the requirements")
    void newProjectIncorrectPayload() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                null,
                "Test Description 1",
                List.of("Java"),
                null,
                null,
                false
        );
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Please provide the project name", "The project name cannot be blank"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(
                VALIDATION_ERROR_MESSAGE,
                List.of(errors)

        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method (create new) is successful when the user has ADMIN role")
    void newProjectSuccessful() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Test Project 1",
                "Test Description 1",
                List.of("Java"),
                "https://github.com/FelipeFranke5/test-backEnd-Java",
                "https://github.com/FelipeFranke5/test-backEnd-Java",
                false
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNoContent());
        List<Project> projects = repository.findAll();
        assertFalse(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method (create new) returns 403 if the user doesn't have ADMIN role")
    void newProjectReturns403WhenRegularUserAuthIsProvided() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Test Project 1",
                "Test Description 1",
                List.of("Java"),
                null,
                null,
                false
        );
        mockMvc
                .perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("regular_user").password("pass").roles("USER"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method (create new) returns 401 if no auth is provided")
    void newProjectReturns401WhenNoAuthIsProvided() throws Exception {
        repository.deleteAll();
        ProjectRequest requestBody = new ProjectRequest(
                "Test Project 1",
                "Test Description 1",
                List.of("Java"),
                null,
                null,
                false
        );
        mockMvc
            .perform(post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isUnauthorized());
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the GET method for a single project returns 404 when the project does not exist")
    void getProjectReturns404WhenNotFound() throws Exception {
        repository.deleteAll();
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", PROJECT_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        responseMap.put("reason", "Project not found with ID '" + nonExistentId + "'");
        String url = URI.create("/api/projects/" + nonExistentId).toString();
        mockMvc
            .perform(get(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(responseMap)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the GET method for a single project returns the correct ProjectReadOnlyDetailed instance")
    void getProjectReturnsCorrectDetailedInstance() throws Exception {
        repository.deleteAll();
        String description = "a".repeat(60);
        Project project1 = new Project();
        project1.setName("Project 1");
        project1.setDescription(description);
        project1.setStack(Arrays.asList("Java", "Maven"));
        project1 = repository.save(project1);
        ProjectReadOnlyDetailed expectedProjectDTO = new ProjectReadOnlyDetailed(
                project1.getId(),
                project1.getName(),
                project1.getDescription(),
                project1.getStack(),
                project1.getGithubURL(),
                project1.getDemoURL(),
                project1.isFeatured(),
                project1.getCreatedAt()
        );
        String url = URI.create("/api/projects/" + project1.getId().toString()).toString();
        mockMvc
            .perform(get(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedProjectDTO)));
        List<Project> projects = repository.findAll();
        assertFalse(projects.isEmpty());
    }

    @Test
    @DisplayName("Assert that the description is rendered correctly to respect its max length")
    void listWithCalculatedDescriptionMaxLength() throws Exception {
        repository.deleteAll();
        // First project
        Project project1 = new Project();
        project1.setName("Project 1");
        project1.setDescription("a".repeat(60));
        project1.setStack(Arrays.asList("Java", "Maven"));
        project1 = repository.save(project1);
        ProjectReadOnly projectDTO1 = new ProjectReadOnly(
                project1.getId(),
                project1.getName(),
                project1.getDescription().substring(0, DESCRIPTION_MAX_LENGTH) + "...",
                project1.getStack()
        );

        // Second project
        Project project2 = new Project();
        project2.setName("Project 2");
        project2.setDescription("Description 2");
        project2.setStack(Arrays.asList("Java", "Maven"));
        project2 = repository.save(project2);
        ProjectReadOnly projectDTO2 = new ProjectReadOnly(
                project2.getId(),
                project2.getName(),
                project2.getDescription(),
                project2.getStack()
        );

        // List of Project DTOs
        List<ProjectReadOnly> expectedList = List.of(projectDTO1, projectDTO2);
        mockMvc
            .perform(get(URI.create("/api/projects"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedList)));
        List<Project> projects = repository.findAll();
        assertFalse(projects.isEmpty());
        assertTrue(projects.size() > 1);
    }

    @Test
    @DisplayName("Assert that the GET method returns the correct List of projects")
    void listWithExistingProjects() throws Exception {
        repository.deleteAll();
        // First project
        Project project1 = new Project();
        project1.setName("Project 1");
        project1.setDescription("Description 1");
        project1.setStack(Arrays.asList("Java", "Maven"));
        project1 = repository.save(project1);
        ProjectReadOnly projectDTO1 = new ProjectReadOnly(
                project1.getId(),
                project1.getName(),
                project1.getDescription(),
                project1.getStack()
        );

        // Second project
        Project project2 = new Project();
        project2.setName("Project 2");
        project2.setDescription("Description 2");
        project2.setStack(Arrays.asList("Java", "Maven"));
        project2 = repository.save(project2);
        ProjectReadOnly projectDTO2 = new ProjectReadOnly(
                project2.getId(),
                project2.getName(),
                project2.getDescription(),
                project2.getStack()
        );

        // List of Project DTOs
        List<ProjectReadOnly> expectedList = List.of(projectDTO1, projectDTO2);
        mockMvc
            .perform(get(URI.create("/api/projects"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedList)));
        List<Project> projects = repository.findAll();
        assertFalse(projects.isEmpty());
        assertTrue(projects.size() > 1);
    }

    @Test
    @DisplayName("Assert that the GET method returns a empty list when no records are saved")
    void emptyListOfProjects() throws Exception {
        repository.deleteAll();
        List<ProjectReadOnly> expectedList = List.of();
        mockMvc
            .perform(get(URI.create("/api/projects"))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(objectMapper.writeValueAsString(expectedList)));
        List<Project> projects = repository.findAll();
        assertTrue(projects.isEmpty());
    }
}
