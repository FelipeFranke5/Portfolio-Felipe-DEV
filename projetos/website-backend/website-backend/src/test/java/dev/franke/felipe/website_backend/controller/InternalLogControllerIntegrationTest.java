package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.model.InternalLog;
import dev.franke.felipe.website_backend.repository.InternalLogRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class InternalLogControllerIntegrationTest {

    private static final String INTERNAL_LOG_NOT_FOUND_EXCEPTION_ERROR_MESSAGE =
            "Unable to find InternalLog you attempted to retrieve. Returning 404";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InternalLogRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.0");

    private InternalLog buildLog(String simpleClassName, String errorMessage) {
        InternalLog log = new InternalLog();
        log.setSimpleClassName(simpleClassName);
        log.setErrorMessage(errorMessage);
        log.setStackTrace("[]");
        return log;
    }

    @Test
    @DisplayName("Assert that the PSQL container runs")
    void loads() {
        assertTrue(postgreSQLContainer.isCreated());
        assertTrue(postgreSQLContainer.isRunning());
    }

    @Test
    @DisplayName("Assert that the GET method as ADMIN returns 200 with the correct InternalLog when a single log exists")
    void listWithOneExistingLog() throws Exception {
        repository.deleteAll();
        InternalLog saved = repository.save(buildLog("RuntimeException", "Something went wrong"));

        mockMvc
                .perform(get(URI.create("/api/internal_log"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(saved))));
    }

    @Test
    @DisplayName("Assert that the GET method as ADMIN returns the most recently created log first when multiple logs exist")
    void listOrdersMostRecentLogFirst() throws Exception {
        repository.deleteAll();
        InternalLog older = repository.save(buildLog("IllegalStateException", "First failure"));
        Thread.sleep(10);
        InternalLog newer = repository.save(buildLog("IllegalArgumentException", "Second failure"));

        String responseJson = mockMvc
                .perform(get(URI.create("/api/internal_log"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<InternalLog> actual = objectMapper.readValue(responseJson, objectMapper.getTypeFactory()
                .constructCollectionType(List.class, InternalLog.class));
        assertEquals(2, actual.size());
        assertEquals(newer.getId(), actual.get(0).getId());
        assertEquals(older.getId(), actual.get(1).getId());
    }

    @Test
    @DisplayName("Assert that the GET method as ADMIN returns an empty list when no records are saved")
    void emptyListOfLogs() throws Exception {
        repository.deleteAll();
        List<InternalLog> expectedList = List.of();

        mockMvc
                .perform(get(URI.create("/api/internal_log"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedList)));
    }

    @Test
    @DisplayName("Assert that the GET method returns 401 when no authentication is provided")
    void getRecentLogsReturns401WhenNoAuthIsProvided() throws Exception {
        mockMvc
                .perform(get(URI.create("/api/internal_log")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Assert that the GET method returns 403 if the user doesn't have ADMIN role")
    void getRecentLogsReturns403WhenRegularUserAuthIsProvided() throws Exception {
        mockMvc
                .perform(get(URI.create("/api/internal_log"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("regular_user").password("pass").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Assert that the GET method for a single log as ADMIN returns the correct InternalLog instance")
    void getLogReturnsCorrectInstance() throws Exception {
        repository.deleteAll();
        InternalLog saved = repository.save(buildLog("RuntimeException", "Something went wrong"));
        String url = URI.create("/api/internal_log/" + saved.getId()).toString();

        mockMvc
                .perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(saved)));
    }

    @Test
    @DisplayName("Assert that the GET method for a single log as ADMIN returns 404 when the log does not exist")
    void getLogReturns404WhenNotFound() throws Exception {
        repository.deleteAll();
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        Map<String, Object> expected = new HashMap<>();
        expected.put("error", INTERNAL_LOG_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        expected.put("reason", "InternalLog not found with ID: " + nonExistentId);
        String url = URI.create("/api/internal_log/" + nonExistentId).toString();

        mockMvc
                .perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    @DisplayName("Assert that the GET method for a single log as ADMIN returns 400 when the given id is not a valid UUID")
    void getLogReturns400WhenIdIsNotUuid() throws Exception {
        String invalidId = "not-a-uuid";
        String url = URI.create("/api/internal_log/" + invalidId).toString();

        mockMvc
                .perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("admin_user").password("pass").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Assert that the GET method for a single log returns 401 when no authentication is provided")
    void getLogReturns401WhenNoAuthIsProvided() throws Exception {
        String id = "00000000-0000-0000-0000-000000000000";
        String url = URI.create("/api/internal_log/" + id).toString();

        mockMvc
                .perform(get(url).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Assert that the GET method for a single log returns 403 if the user doesn't have ADMIN role")
    void getLogReturns403WhenRegularUserAuthIsProvided() throws Exception {
        String id = "00000000-0000-0000-0000-000000000000";
        String url = URI.create("/api/internal_log/" + id).toString();

        mockMvc
                .perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("regular_user").password("pass").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
