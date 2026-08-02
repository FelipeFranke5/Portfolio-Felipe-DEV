package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.config.SecurityConfig;
import dev.franke.felipe.website_backend.dto.SkillDTO;
import dev.franke.felipe.website_backend.dto.SkillLevel;
import dev.franke.felipe.website_backend.dto.SkillRequest;
import dev.franke.felipe.website_backend.dto.UnprocessableEntityResponse;
import dev.franke.felipe.website_backend.exception.SkillException;
import dev.franke.felipe.website_backend.exception.SkillNotFoundException;
import dev.franke.felipe.website_backend.service.InternalLogService;
import dev.franke.felipe.website_backend.service.SkillService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SkillController.class)
@Import(SecurityConfig.class)
class SkillControllerTest {

    private static final String VALIDATION_ERROR_MESSAGE =
            "The request has failed due to incorrect information in the payload provided.";

    private static final String SKILL_EXCEPTION_ERROR_MESSAGE =
            "The request has failed due to incorrect use of SKILL endpoint(s). Please " +
            "review the details in the response body to correct your request.";

    private static final String SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE =
            "Unable to find the skill you attempted to retrieve. Returning 404";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private SkillService skillService;

    @MockitoBean
    private InternalLogService internalLogService;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SkillRequest validRequest() {
        return new SkillRequest("Java", "Backend", 5);
    }

    @Test
    @DisplayName("GET /api/skills returns 200 with the list produced by the service")
    void getSkillsReturnsOkAndBody() throws Exception {
        SkillDTO dto = new SkillDTO(UUID.randomUUID(), "Java", "Backend", SkillLevel.WORK_EXPERIENCE);
        when(skillService.getSkills()).thenReturn(List.of(dto));
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(dto))));
    }

    @Test
    @DisplayName("GET /api/skills returns 200 with an empty array when the service returns no skills")
    void getSkillsReturnsEmptyList() throws Exception {
        when(skillService.getSkills()).thenReturn(List.of());
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("GET /api/skills/{id} returns 200 with the SkillDTO produced by the service")
    void retrieveSkillReturnsOkAndBody() throws Exception {
        UUID skillId = UUID.randomUUID();
        String idStr = skillId.toString();
        SkillDTO dto = new SkillDTO(skillId, "Angular", "Frontend", SkillLevel.INTERMEDIATE_KNOWLEDGE);
        when(skillService.getSkillById(idStr)).thenReturn(dto);

        mockMvc.perform(get("/api/skills/" + skillId))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(dto)));
    }

    @Test
    @DisplayName("GET /api/skills/{id} returns 404 with the standard error body when the service reports the skill as not found")
    void retrieveSkillReturns404WhenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(skillService.getSkillById(id))
                .thenThrow(new SkillNotFoundException("Skill with id: " + id + " not found"));
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Skill with id: " + id + " not found");
        mockMvc.perform(get("/api/skills/" + id))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("GET /api/skills/{id} returns 400 when the given id is not a parsable UUID")
    void retrieveSkillReturns400WhenIdIsUnparsable() throws Exception {
        String invalidId = "not-a-uuid";
        when(skillService.getSkillById(invalidId))
                .thenThrow(new SkillException("Unparsable ID: " + invalidId));
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Unparsable ID: " + invalidId);
        mockMvc.perform(get("/api/skills/" + invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("POST /api/skills as ADMIN with a valid payload returns 204 and delegates to the service")
    void registerSkillSuccessful() throws Exception {
        SkillRequest request = validRequest();
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        verify(skillService, times(1)).saveSkill(request);
    }

    @Test
    @DisplayName("POST /api/skills returns 401 when no authentication is provided")
    void registerSkillReturns401WhenNoAuth() throws Exception {
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("POST /api/skills returns 403 when the authenticated user does not have the ADMIN role")
    void registerSkillReturns403WhenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("regular_user").password("pass").roles("USER"))
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("POST /api/skills returns 422 when the name is blank, and never calls the service")
    void registerSkillBlankNameReturns422() throws Exception {
        SkillRequest invalidRequest = new SkillRequest("", "Backend", 5);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("POST /api/skills returns 422 when the category is blank, and never calls the service")
    void registerSkillBlankCategoryReturns422() throws Exception {
        SkillRequest invalidRequest = new SkillRequest("Java", "", 5);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("category", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("POST /api/skills returns 422 when the name exceeds 50 characters")
    void registerSkillNameTooLongReturns422() throws Exception {
        SkillRequest invalidRequest = new SkillRequest("a".repeat(51), "Backend", 5);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Cannot exceed 50 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("POST /api/skills returns 422 when the category exceeds 50 characters")
    void registerSkillCategoryTooLongReturns422() throws Exception {
        SkillRequest invalidRequest = new SkillRequest("Java", "a".repeat(51), 5);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("category", List.of("Cannot exceed 50 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("POST /api/skills returns 422 when the level is zero or negative (not positive)")
    void registerSkillLevelNotPositiveReturns422() throws Exception {
        SkillRequest invalidRequest = new SkillRequest("Java", "Backend", 0);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("level", List.of("Should be positive"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("POST /api/skills returns 422 when the level is above the allowed maximum (5)")
    void registerSkillLevelAboveMaxReturns422() throws Exception {
        SkillRequest invalidRequest = new SkillRequest("Java", "Backend", 6);
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("level", List.of("Range allowed: 1 to 5"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("POST /api/skills returns 400 with the standard error body when the service reports a duplicate name")
    void registerSkillReturns400WhenServiceThrowsSkillException() throws Exception {
        SkillRequest request = validRequest();
        doThrow(new SkillException("Skill with name " + request.name() + " already exists"))
                .when(skillService).saveSkill(request);
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Skill with name " + request.name() + " already exists");
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("PUT /api/skills/{id} as ADMIN with a valid payload returns 204 and delegates to the service")
    void updateSkillSuccessful() throws Exception {
        String id = UUID.randomUUID().toString();
        SkillRequest request = validRequest();
        mockMvc.perform(put("/api/skills/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        verify(skillService, times(1)).updateSkill(id, request);
    }

    @Test
    @DisplayName("PUT /api/skills/{id} returns 404 with the standard error body when the service reports the skill as not found")
    void updateSkillReturns404WhenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        SkillRequest request = validRequest();
        doThrow(new SkillNotFoundException("Skill with id: " + id + " not found"))
                .when(skillService).updateSkill(id, request);
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Skill with id: " + id + " not found");
        mockMvc.perform(put("/api/skills/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("PUT /api/skills/{id} returns 422 when the payload fails bean validation, and never touches the service")
    void updateSkillInvalidPayloadReturns422() throws Exception {
        String id = UUID.randomUUID().toString();
        SkillRequest invalidRequest = new SkillRequest(null, null, 0);
        mockMvc.perform(put("/api/skills/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent());
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("PUT /api/skills/{id} returns 403 when the authenticated user does not have the ADMIN role")
    void updateSkillReturns403WhenNotAdmin() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(put("/api/skills/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("regular_user").password("pass").roles("USER"))
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("PUT /api/skills/{id} returns 401 when no authentication is provided")
    void updateSkillReturns401WhenNoAuth() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(put("/api/skills/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("DELETE /api/skills/{id} as ADMIN returns 204 and delegates to the service")
    void deleteSkillSuccessful() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(delete("/api/skills/" + id)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isNoContent());
        verify(skillService, times(1)).deleteSkill(id);
    }

    @Test
    @DisplayName("DELETE /api/skills/{id} returns 404 with the standard error body when the service reports the skill as not found")
    void deleteSkillReturns404WhenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        doThrow(new SkillNotFoundException("Skill with id: " + id + " not found"))
                .when(skillService).deleteSkill(id);
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "Skill with id: " + id + " not found");
        mockMvc.perform(delete("/api/skills/" + id)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("DELETE /api/skills/{id} returns 403 when the authenticated user does not have the ADMIN role")
    void deleteSkillReturns403WhenNotAdmin() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(delete("/api/skills/" + id)
                        .with(user("regular_user").password("pass").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(skillService);
    }

    @Test
    @DisplayName("DELETE /api/skills/{id} returns 401 when no authentication is provided")
    void deleteSkillReturns401WhenNoAuth() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(delete("/api/skills/" + id))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(skillService);
    }
}
