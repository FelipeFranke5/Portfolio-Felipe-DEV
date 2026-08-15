package dev.franke.felipe.website_backend.repository;

import dev.franke.felipe.website_backend.model.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
class ContactRepositoryTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ContactRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.0");

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private Contact newContact(String name, boolean sent, int retryCount) {
        Contact contact = new Contact();
        contact.setName(name);
        contact.setEmail(name.toLowerCase().replace(" ", ".") + "@email.com");
        contact.setMessage("I am test message " + name);
        contact.setSent(sent);
        contact.setRetryCount((short) retryCount);
        return repository.saveAndFlush(contact);
    }

    private void forceCreatedAt(UUID id, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE contact_messages SET created_at = ? WHERE id = ?", createdAt, id);
    }

    @Test
    @DisplayName(
        "When there is only 1 elegible pending Contact, calling findPendingMessages " +
        "must return a List containing only the elegible Contact"
    )
    void findPendingMessagesReturnsOnlyEligibleMessages() {
        Contact eligible = newContact("Elegible", false, 2);
        newContact("Already Sent", true, 0);
        newContact("Max Retries Reached", false, 4);

        List<Contact> result = repository.findPendingMessages();

        assertEquals(1, result.size());
        assertEquals(eligible.getId(), result.getFirst().getId());
    }

    @Test
    @DisplayName(
        "When there is only 1 elegible pending Contact and its current retryCount " +
        "equals '3' (on limit), calling findPendingMessages must return a List " +
        "containing only the elegible Contact"
    )
    void findPendingMessagesIncludesBoundaryRetryCount() {
        Contact boundary = newContact("On Limit", false, 3);

        List<Contact> result = repository.findPendingMessages();

        assertEquals(1, result.size());
        assertEquals(boundary.getId(), result.getFirst().getId());
    }

    @Test
    @DisplayName(
        "When there is no elegible Contact, calling findPendingMessages must return " +
        "a empty list"
    )
    void findPendingMessagesReturnsEmptyWhenNoneEligible() {
        newContact("Already Sent", true, 0);
        newContact("Max Retries Reached", false, 5);
        assertTrue(repository.findPendingMessages().isEmpty());
    }

    @Test
    @DisplayName(
        "Assert that calling findPendingMessages returns a list that is " +
        "ordered by the createdAt (from newest to oldest)"
    )
    void findPendingMessagesOrdersByCreatedAtDesc() {
        Contact oldest = newContact("Oldest", false, 0);
        Contact middle = newContact("Middle", false, 0);
        Contact newest = newContact("Newest", false, 0);

        forceCreatedAt(oldest.getId(), LocalDateTime.now().minusDays(3));
        forceCreatedAt(middle.getId(), LocalDateTime.now().minusDays(2));
        forceCreatedAt(newest.getId(), LocalDateTime.now().minusDays(1));

        List<Contact> result = repository.findPendingMessages();

        assertEquals(3, result.size());
        assertEquals(newest.getId(), result.get(0).getId());
        assertEquals(middle.getId(), result.get(1).getId());
        assertEquals(oldest.getId(), result.get(2).getId());
    }

    @Test
    @DisplayName(
        "When there are Contact messages already sent AND pending messages, " +
        "calling deleteAlreadySentMessages must delete ONLY messages with sent=true"
    )
    void deleteAlreadySentMessagesRemovesOnlySentOnes() {
        newContact("Already Sent", true, 0);
        Contact pending = newContact("Pending", false, 0);

        repository.deleteAlreadySentMessages();

        List<Contact> remaining = repository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(pending.getId(), remaining.getFirst().getId());
    }

    @Test
    @DisplayName(
        "When there are only pending Contact messages, calling " +
        "deleteAlreadySentMessages must keep the messages"
    )
    void deleteAlreadySentMessagesNoOpWhenNoneSent() {
        newContact("Pending", false, 0);
        newContact("Pending 2", false, 0);
        newContact("Pending 3", false, 0);
        newContact("Pending 4", false, 0);

        repository.deleteAlreadySentMessages();

        assertEquals(4, repository.findAll().size());
    }

    @Test
    @DisplayName(
        "When there are SENT Contact messages, calling " +
        "deleteAlreadySentMessages must delete all messages " +
        "where sent=true, while keeping messages where sent=false"
    )
    void deleteAlreadySentMessagesRemovesAllWhenAllSent() {
        newContact("Sent 1", true, 0);
        newContact("Sent 2", true, 1);
        newContact("Sent 3", true, 2);
        newContact("Sent 4", true, 1);
        newContact("Sent 5", true, 3);
        newContact("Sent 6", true, 1);
        newContact("Not Sent 1", false, 1);
        newContact("Not Sent 2", false, 3);

        repository.deleteAlreadySentMessages();

        assertEquals(2, repository.findAll().size());
    }

    @Test
    @DisplayName(
        "When there is a Contact message not considered old, calling " +
        "deleteOldMessages must keep the Contact message"
    )
    void deleteOldMessagesKeepsMessageWithinBoundary() {
        Contact boundary = newContact("Within Boundary", false, 0);
        forceCreatedAt(boundary.getId(), LocalDateTime.now().minusDays(13).minusHours(23));

        repository.deleteOldMessages();

        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName(
        "When there are old Contact messages, calling deleteOldMessages " +
        "must delete all of them"
    )
    void deleteOldMessagesDeletes() {
        Contact old1 = newContact("Old 1", false, 0);
        forceCreatedAt(old1.getId(), LocalDateTime.now().minusDays(20));
        Contact old2 = newContact("Old 2", true, 0);
        forceCreatedAt(old2.getId(), LocalDateTime.now().minusDays(30));
        Contact old3 = newContact("Old 3", true, 2);
        forceCreatedAt(old3.getId(), LocalDateTime.now().minusDays(30));
        Contact old4 = newContact("Old 3", false, 4);
        forceCreatedAt(old4.getId(), LocalDateTime.now().minusDays(70));

        repository.deleteOldMessages();
        System.out.println("DEBUG::::: REPOSITORY FIND ALL: " + repository.findAll());

        assertTrue(repository.findAll().isEmpty());
    }
}
