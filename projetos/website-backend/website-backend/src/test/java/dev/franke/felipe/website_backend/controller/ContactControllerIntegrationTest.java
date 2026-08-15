package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.dto.ContactRequest;
import dev.franke.felipe.website_backend.dto.UnprocessableEntityResponse;
import dev.franke.felipe.website_backend.model.Contact;
import dev.franke.felipe.website_backend.repository.ContactRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class ContactControllerIntegrationTest {

    private static final String VALIDATION_ERROR_MESSAGE =
            "The request has failed due to incorrect information in the payload provided.";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.0");

    private ContactRequest validRequest() {
        return new ContactRequest("John Doe", "john@email.com", "Hello, I would like to know more about your services.");
    }

    @Test
    @DisplayName("Assert that the PSQL container runs")
    void loads() {
        assertTrue(postgreSQLContainer.isCreated());
        assertTrue(postgreSQLContainer.isRunning());
    }

    @Test
    @DisplayName("Assert that the POST method persists a new Contact with sent=false and retryCount=0, without requiring authentication")
    void newContactSuccessful() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = validRequest();

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated());

        List<Contact> contacts = repository.findAll();
        assertEquals(1, contacts.size());
        Contact saved = contacts.getFirst();
        assertEquals(requestBody.name(), saved.getName());
        assertEquals(requestBody.email(), saved.getEmail());
        assertEquals(requestBody.message(), saved.getMessage());
        assertFalse(saved.isSent());
        assertEquals(0, saved.getRetryCount());
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Assert that the POST method response body's messageId matches the id actually persisted in the Database")
    void newContactResponseBodyMatchesPersistedEntity() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = validRequest();

        String responseJson = mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Contact saved = repository.findAll().getFirst();
        Map<?, ?> responseBody = objectMapper.readValue(responseJson, Map.class);
        assertEquals(saved.getId().toString(), responseBody.get("messageId"));
        assertNotNull(responseBody.get("createdAt"));
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the name is blank, and persists nothing")
    void newContactBlankName() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = new ContactRequest("", "john@email.com", "Valid message");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the name has more than 100 characters")
    void newContactNameTooLong() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = new ContactRequest("a".repeat(101), "john@email.com", "Valid message");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Cannot exceed 100 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the email is blank")
    void newContactBlankEmail() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = new ContactRequest("John Doe", "", "Valid message");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("email", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the email has an invalid format")
    void newContactInvalidEmailFormat() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = new ContactRequest("John Doe", "not-an-email", "Valid message");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("email", List.of("Must be a valid Email"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the message is blank")
    void newContactBlankMessage() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = new ContactRequest("John Doe", "john@email.com", "");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("message", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 when the message has more than 3000 characters")
    void newContactMessageTooLong() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = new ContactRequest("John Doe", "john@email.com", "a".repeat(3001));
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("message", List.of("Cannot exceed 3000 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that the POST method returns 422 grouping all field errors when every field is invalid, and persists nothing")
    void newContactMultipleInvalidFields() throws Exception {
        repository.deleteAll();
        ContactRequest requestBody = new ContactRequest("", "", "");

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnprocessableContent());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Assert that consecutive valid POSTs each create an independent Contact record")
    void multipleContactsArePersistedIndependently() throws Exception {
        repository.deleteAll();
        ContactRequest first = new ContactRequest("John", "john@email.com", "First message");
        ContactRequest second = new ContactRequest("Jane", "jane@email.com", "Second message");

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        List<Contact> contacts = repository.findAll();
        assertEquals(2, contacts.size());
        assertTrue(contacts.stream().anyMatch(c -> c.getName().equals("John")));
        assertTrue(contacts.stream().anyMatch(c -> c.getName().equals("Jane")));
    }
}
