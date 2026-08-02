package dev.franke.felipe.website_backend.controller;

import dev.franke.felipe.website_backend.config.SecurityConfig;
import dev.franke.felipe.website_backend.dto.ContactRequest;
import dev.franke.felipe.website_backend.dto.ContactResponse;
import dev.franke.felipe.website_backend.dto.UnprocessableEntityResponse;
import dev.franke.felipe.website_backend.service.ContactService;
import dev.franke.felipe.website_backend.service.InternalLogService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactController.class)
@Import(SecurityConfig.class)
class ContactControllerTest {

    private static final String VALIDATION_ERROR_MESSAGE =
            "The request has failed due to incorrect information in the payload provided.";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private InternalLogService internalLogService;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ContactRequest validRequest() {
        return new ContactRequest("John Doe", "johndoe@email.com", "Hello, I would like to know more about your services.");
    }

    @Test
    @DisplayName("POST /api/contact with a valid payload returns 201 with the body produced by the service")
    void saveContactSuccessful() throws Exception {
        ContactRequest request = validRequest();
        ContactResponse response = new ContactResponse(UUID.randomUUID(), LocalDateTime.now());
        when(contactService.saveContact(request)).thenReturn(response);

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(contactService, times(1)).saveContact(request);
    }

    @Test
    @DisplayName("POST /api/contact does not require authentication - it is a public endpoint")
    void saveContactDoesNotRequireAuthentication() throws Exception {
        ContactRequest request = validRequest();
        when(contactService.saveContact(request)).thenReturn(new ContactResponse(UUID.randomUUID(), LocalDateTime.now()));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/contact returns 422 when name is blank, and never calls the service")
    void saveContactBlankNameReturns422() throws Exception {
        ContactRequest invalidRequest = new ContactRequest("", "johndoe@email.com", "Valid message");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("POST /api/contact returns 422 when name exceeds 100 characters")
    void saveContactNameTooLongReturns422() throws Exception {
        ContactRequest invalidRequest = new ContactRequest("a".repeat(101), "johndoe@email.com", "Valid message");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("name", List.of("Cannot exceed 100 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("POST /api/contact returns 422 when email is blank, and never calls the service")
    void saveContactBlankEmailReturns422() throws Exception {
        ContactRequest invalidRequest = new ContactRequest("John Doe", "", "Valid message");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("email", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("POST /api/contact returns 422 when email is not a valid address format")
    void saveContactInvalidEmailFormatReturns422() throws Exception {
        ContactRequest invalidRequest = new ContactRequest("John Doe", "not-an-email", "Valid message");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("email", List.of("Must be a valid Email"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("POST /api/contact returns 422 when message is blank, and never calls the service")
    void saveContactBlankMessageReturns422() throws Exception {
        ContactRequest invalidRequest = new ContactRequest("John Doe", "johndoe@email.com", "");
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("message", List.of("Field is required"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("POST /api/contact returns 422 when message exceeds 3000 characters")
    void saveContactMessageTooLongReturns422() throws Exception {
        ContactRequest invalidRequest = new ContactRequest("John Doe", "johndoe@email.com", "a".repeat(3001));
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("message", List.of("Cannot exceed 3000 characters"));
        UnprocessableEntityResponse expected = new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, List.of(errors));

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
        verifyNoInteractions(contactService);
    }

    @Test
    @DisplayName("POST /api/contact returns 422 when every field is invalid at once, grouping all errors, and never calls the service")
    void saveContactMultipleInvalidFieldsReturns422() throws Exception {
        ContactRequest invalidRequest = new ContactRequest("", "", "");

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableContent());
        verifyNoInteractions(contactService);
    }
}
