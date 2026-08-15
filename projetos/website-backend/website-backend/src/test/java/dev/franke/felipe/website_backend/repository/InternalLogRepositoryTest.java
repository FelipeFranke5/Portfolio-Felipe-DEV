package dev.franke.felipe.website_backend.repository;

import dev.franke.felipe.website_backend.model.InternalLog;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
class InternalLogRepositoryTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private InternalLogRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16.0");

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private InternalLog newLog(String suffix) {
        InternalLog log = new InternalLog();
        log.setSimpleClassName("TestException" + suffix);
        log.setErrorMessage("Test Error Message " + suffix);
        log.setStackTrace("stack trace " + suffix);
        return repository.saveAndFlush(log);
    }

    private void forceCreatedAt(UUID id, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE internal_log SET created_at = ? WHERE id = ?", createdAt, id);
    }

    @Test
    @DisplayName("When there are 0 results in DB, calling findFirst100Results must return a empty list")
    void findFirst100ResultsReturnsEmptyWhenNoneExist() {
        assertTrue(repository.findFirst100Results().isEmpty());
    }

    @Test
    @DisplayName(
        "When there are results in DB, calling findFirst100Results must return " +
        "a List of them. Order by the createdAt (newest first)"
    )
    void findFirst100ResultsOrdersByCreatedAtDesc() {
        InternalLog oldest = newLog("Oldest");
        InternalLog middle = newLog("Middle");
        InternalLog newest = newLog("Newest");

        forceCreatedAt(oldest.getId(), LocalDateTime.now().minusDays(3));
        forceCreatedAt(middle.getId(), LocalDateTime.now().minusDays(2));
        forceCreatedAt(newest.getId(), LocalDateTime.now().minusDays(1));

        List<InternalLog> result = repository.findFirst100Results();

        assertEquals(3, result.size());
        assertEquals(newest.getId(), result.get(0).getId());
        assertEquals(middle.getId(), result.get(1).getId());
        assertEquals(oldest.getId(), result.get(2).getId());
    }

    @Test
    @DisplayName(
        "When there are more than 100 results in DB, calling findFirst100Results must return " +
        "a List of only the first 100 results. We don't negotiate that"
    )
    void findFirst100ResultsLimitsTo100WhenMoreExist() {
        List<InternalLog> inserted = new ArrayList<>();

        for (int i = 0; i < 105; i++) {
            InternalLog log = newLog(String.valueOf(i));
            forceCreatedAt(log.getId(), LocalDateTime.now().minusMinutes(i));
            inserted.add(log);
        }

        List<InternalLog> result = repository.findFirst100Results();

        assertEquals(100, result.size());
        assertEquals(inserted.getFirst().getId(), result.getFirst().getId());
    }

    @Test
    @DisplayName("When there are old logs, calling deleteOldLogs must delete old logs")
    void deleteOldLogsRemovesLogsOlderThan90Days() {
        InternalLog old = newLog("Old");
        forceCreatedAt(old.getId(), LocalDateTime.now().minusDays(91));

        repository.deleteOldLogs();

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName(
        "When there are logs that aren't older than 90 days, calling deleteOldLogs " +
        "shouldn't delete them"
    )
    void deleteOldLogsKeepsLogWithinBoundary() {
        InternalLog boundary = newLog("DentroDoLimite");
        forceCreatedAt(boundary.getId(), LocalDateTime.now().minusDays(89).minusHours(23));

        repository.deleteOldLogs();

        List<InternalLog> remaining = repository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(boundary.getId(), remaining.getFirst().getId());
    }

    @Test
    @DisplayName("Recent logs shouldn't be deleted by calling deleteOldLogs")
    void deleteOldLogsNoOpWhenNoneOld() {
        newLog("Recent");

        repository.deleteOldLogs();

        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName(
        "When there are recent AND old logs in the system, calling " +
        "deleteOldLogs should only delete the old logs"
    )
    void deleteOldLogsRemovesOnlyOldOnesKeepingRecent() {
        InternalLog old = newLog("Old");
        InternalLog recent = newLog("Recent");

        forceCreatedAt(old.getId(), LocalDateTime.now().minusDays(120));
        forceCreatedAt(recent.getId(), LocalDateTime.now().minusDays(1));

        repository.deleteOldLogs();

        List<InternalLog> remaining = repository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(recent.getId(), remaining.getFirst().getId());
    }
}
