package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.config.SecurityConfig;
import dev.franke.felipe.website_backend.exception.InternalLogException;
import dev.franke.felipe.website_backend.exception.InternalLogNotFoundException;
import dev.franke.felipe.website_backend.model.InternalLog;
import dev.franke.felipe.website_backend.service.InternalLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalLogController.class)
@Import(SecurityConfig.class)
class InternalLogControllerTest {

    private static final String INTERNAL_LOG_EXCEPTION_ERROR_MESSAGE =
            "The request has failed due to incorrect use of INTERNAL_LOG endpoint(s). Please " +
            "review the details in the response body to correct your request.";

    private static final String INTERNAL_LOG_NOT_FOUND_EXCEPTION_ERROR_MESSAGE =
            "Unable to find InternalLog you attempted to retrieve. Returning 404";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private InternalLogService internalLogService;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private InternalLog buildLog() {
        return new InternalLog(
                UUID.randomUUID(),
                "RuntimeException",
                "simulated failure",
                "[]",
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("GET /api/internal_log as ADMIN returns 200 with the list produced by the service")
    void getRecentLogsReturnsOkAndBody() throws Exception {
        InternalLog log = buildLog();
        when(internalLogService.getFirstLogs()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/internal_log")
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(log))));
    }

    @Test
    @DisplayName("GET /api/internal_log as ADMIN returns 200 with an empty array when the service returns no logs")
    void getRecentLogsReturnsEmptyList() throws Exception {
        when(internalLogService.getFirstLogs()).thenReturn(List.of());

        mockMvc.perform(get("/api/internal_log")
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("GET /api/internal_log returns 401 when no authentication is provided")
    void getRecentLogsReturns401WhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/internal_log"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(internalLogService);
    }

    @Test
    @DisplayName("GET /api/internal_log returns 403 when the authenticated user does not have the ADMIN role")
    void getRecentLogsReturns403WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/internal_log")
                        .with(user("regular_user").password("pass").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(internalLogService);
    }

    @Test
    @DisplayName("GET /api/internal_log/{id} as ADMIN returns 200 with the InternalLog produced by the service")
    void getLogByIdReturnsOkAndBody() throws Exception {
        InternalLog log = buildLog();
        String id = log.getId().toString();
        when(internalLogService.getLogById(id)).thenReturn(log);

        mockMvc.perform(get("/api/internal_log/" + id)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(log)));
    }

    @Test
    @DisplayName("GET /api/internal_log/{id} returns 404 with the standard error body when the service reports the log as not found")
    void getLogByIdReturns404WhenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(internalLogService.getLogById(id))
                .thenThrow(new InternalLogNotFoundException("InternalLog not found with ID: " + id));
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", INTERNAL_LOG_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "InternalLog not found with ID: " + id);

        mockMvc.perform(get("/api/internal_log/" + id)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("GET /api/internal_log/{id} returns 400 with the standard error body when the given id is not a valid UUID")
    void getLogByIdReturns400WhenIdIsUnparsable() throws Exception {
        String invalidId = "not-a-uuid";
        when(internalLogService.getLogById(invalidId))
                .thenThrow(new InternalLogException(invalidId + " is not a valid UUID."));
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", INTERNAL_LOG_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", invalidId + " is not a valid UUID.");

        mockMvc.perform(get("/api/internal_log/" + invalidId)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("GET /api/internal_log/{id} returns 401 when no authentication is provided")
    void getLogByIdReturns401WhenNoAuth() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/internal_log/" + id))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(internalLogService);
    }

    @Test
    @DisplayName("GET /api/internal_log/{id} returns 403 when the authenticated user does not have the ADMIN role")
    void getLogByIdReturns403WhenNotAdmin() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/internal_log/" + id)
                        .with(user("regular_user").password("pass").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(internalLogService);
    }
}
