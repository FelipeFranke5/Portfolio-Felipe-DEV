package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.dto.SkillDTO;
import dev.franke.felipe.website_backend.dto.SkillLevel;
import dev.franke.felipe.website_backend.dto.SkillRequest;
import dev.franke.felipe.website_backend.dto.UnprocessableEntityResponse;
import dev.franke.felipe.website_backend.model.Skill;
import dev.franke.felipe.website_backend.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
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

import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
class SkillControllerIntegrationTest {

    private static final String VALIDATION_ERROR_MESSAGE =
            "The request has failed due to incorrect information in the payload provided.";

    private static final String SKILL_EXCEPTION_ERROR_MESSAGE =
            "The request has failed due to incorrect use of SKILL endpoint(s). Please " +
            "review the details in the response body to correct your request.";

    private static final String SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE =
            "Unable to find the skill you attempted to retrieve. Returning 404";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SkillRepository repository;

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
    void deleteSkill() throws Exception {
        repository.deleteAll();
        Skill skill = new Skill();
        skill.setName("Java");
        skill.setCategory("Backend");
        skill.setLevel((short) 5);
        skill = repository.save(skill);
        String deleteURL = URI.create("/api/skills/" + skill.getId()).toString();

        mockMvc
                .perform(delete(deleteURL)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isNoContent());

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the DELETE method returns 404 when the skill does not exist")
    void deleteSkillReturns404WhenNotFound() throws Exception {
        repository.deleteAll();
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Skill with id: " + nonExistentId + " not found");

        mockMvc
                .perform(delete("/api/skills/" + nonExistentId)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("Assert that the PUT method returns 204 when the update is successful")
    void updateSkill() throws Exception {
        repository.deleteAll();
        Skill skill = new Skill();
        skill.setName("Java");
        skill.setCategory("Backend");
        skill.setLevel((short) 2);
        skill = repository.save(skill);
        SkillRequest requestBody = new SkillRequest("Java Updated", "Backend Senior", 5);
        String updateURL = URI.create("/api/skills/" + skill.getId()).toString();

        mockMvc
                .perform(put(updateURL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNoContent());

        List<Skill> skills = repository.findAll();
        assertEquals(1, skills.size());
        assertEquals(requestBody.name(), skills.getFirst().getName());
        assertEquals(requestBody.category(), skills.getFirst().getCategory());
        assertEquals(requestBody.level(), skills.getFirst().getLevel());
    }

    @Test
    @DisplayName("Assert that the PUT method returns 404 when the skill does not exist")
    void updateSkillReturns404WhenNotFound() throws Exception {
        repository.deleteAll();
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        SkillRequest requestBody = new SkillRequest("Java", "Backend", 5);
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Skill with id: " + nonExistentId + " not found");

        mockMvc
                .perform(put("/api/skills/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("Assert that the POST method returns 400 when the skill with that name already exists")
    void newSkillAlreadyExists() throws Exception {
        repository.deleteAll();
        Skill skill = new Skill();
        skill.setName("Java");
        skill.setCategory("Backend");
        skill.setLevel((short) 5);
        repository.save(skill);
        SkillRequest requestBody = new SkillRequest("Java", "Backend", 4);
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Skill with name " + requestBody.name() + " already exists");

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Name is blank")
    void newSkillBlankName() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("", "Backend", 5);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Category is blank")
    void newSkillBlankCategory() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("Java", "", 5);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("category", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Name has more than 50 characters")
    void newSkillNameTooLong() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("a".repeat(51), "Backend", 5);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Cannot exceed 50 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Category has more than 50 characters")
    void newSkillCategoryTooLong() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("Java", "a".repeat(51), 5);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("category", List.of("Cannot exceed 50 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Level is zero (not positive)")
    void newSkillLevelZero() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("Java", "Backend", 0);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("level", List.of("Should be positive"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Level is negative")
    void newSkillLevelNegative() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("Java", "Backend", -1);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("level", List.of("Should be positive"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the Level is above 5")
    void newSkillLevelAboveMax() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("Java", "Backend", 6);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("level", List.of("Range allowed: 1 to 5"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the payload contains multiple invalid fields")
    void newSkillMultipleInvalidFields() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest(null, null, 0);

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method (create new) is successful when the user has ADMIN role")
    void newSkillSuccessful() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("Java", "Backend", 5);

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNoContent());

        List<Skill> skills = repository.findAll();
        assertEquals(1, skills.size());
        assertEquals(requestBody.name(), skills.getFirst().getName());
        assertEquals(requestBody.category(), skills.getFirst().getCategory());
        assertEquals(requestBody.level(), skills.getFirst().getLevel());
    }

    @Test
    @DisplayName("Assert that the POST method (create new) returns 403 if the user doesn't have ADMIN role")
    void newSkillReturns403WhenRegularUserAuthIsProvided() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("Java", "Backend", 5);

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("regular_user").password("pass").roles("USER"))
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method (create new) returns 401 if no auth is provided")
    void newSkillReturns401WhenNoAuthIsProvided() throws Exception {
        repository.deleteAll();
        SkillRequest requestBody = new SkillRequest("Java", "Backend", 5);

        mockMvc
                .perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
        assertTrue(repository.findAll().isEmpty());
    }

    // ─── GET by id ──────────────────────────────────────────────

    @Test
    @DisplayName("Assert that the GET method for a single skill returns 404 when the skill does not exist")
    void getSkillReturns404WhenNotFound() throws Exception {
        repository.deleteAll();
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Skill with id: " + nonExistentId + " not found");
        String url = URI.create("/api/skills/" + nonExistentId).toString();

        mockMvc
                .perform(get(url).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName(
            "Assert that the GET method for a single skill returns the correct SkillDTO, " +
            "with the numeric level correctly mapped to the SkillLevel enum"
    )
    void getSkillReturnsCorrectDTO() throws Exception {
        repository.deleteAll();
        Skill skill = new Skill();
        skill.setName("Java");
        skill.setCategory("Backend");
        skill.setLevel((short) 5);
        skill = repository.save(skill);
        SkillDTO expectedDTO = new SkillDTO(skill.getId(), skill.getName(), skill.getCategory(), SkillLevel.WORK_EXPERIENCE);
        String url = URI.create("/api/skills/" + skill.getId()).toString();

        mockMvc
                .perform(get(url).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedDTO)));
    }

    @Test
    @DisplayName("Assert that the GET method returns the correct List of skills, with every level correctly mapped")
    void listWithExistingSkills() throws Exception {
        repository.deleteAll();
        Skill skill1 = new Skill();
        skill1.setName("Java");
        skill1.setCategory("Backend");
        skill1.setLevel((short) 5);
        repository.save(skill1);

        Skill skill2 = new Skill();
        skill2.setName("Angular");
        skill2.setCategory("Frontend");
        skill2.setLevel((short) 3);
        repository.save(skill2);

        SkillDTO expectedDTO1 = new SkillDTO(skill1.getId(), "Java", "Backend", SkillLevel.WORK_EXPERIENCE);
        SkillDTO expectedDTO2 = new SkillDTO(skill2.getId(), "Angular", "Frontend", SkillLevel.INTERMEDIATE_KNOWLEDGE);
        List<SkillDTO> expectedList = List.of(expectedDTO1, expectedDTO2);

        mockMvc
                .perform(get(URI.create("/api/skills")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedList)));

        assertEquals(2, repository.findAll().size());
    }

    @Test
    @DisplayName("Assert that the GET method returns an empty list when no records are saved")
    void emptyListOfSkills() throws Exception {
        repository.deleteAll();
        List<SkillDTO> expectedList = List.of();

        mockMvc
                .perform(get(URI.create("/api/skills")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedList)));
        assertTrue(repository.findAll().isEmpty());
    }
}
