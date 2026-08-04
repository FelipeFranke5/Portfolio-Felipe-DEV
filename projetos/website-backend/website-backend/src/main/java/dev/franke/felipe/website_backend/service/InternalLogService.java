package dev.franke.felipe.website_backend.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.franke.felipe.website_backend.exception.InternalLogException;
import dev.franke.felipe.website_backend.exception.InternalLogNotFoundException;
import dev.franke.felipe.website_backend.model.InternalLog;
import dev.franke.felipe.website_backend.repository.InternalLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalLogService {

    private final InternalLogRepository internalLogRepository;

    private static final long PRUNE_OLD_LOGS_RATE_MS = 86400000; // A cada 24 horas

    @Scheduled(fixedRate = PRUNE_OLD_LOGS_RATE_MS)
    @Transactional
    public void pruneOldLogs() {
        log.info("Pruning Old logs from InternalLog");
        internalLogRepository.deleteOldLogs();
        log.info("Old logs prune routine executed!");
    }

    public InternalLog getLogById(String logId) {
        UUID logUUID = null;
        
        try {
            logUUID = UUID.fromString(logId);
        } catch (Exception parsingException) {
            throw new InternalLogException(logId + " is not a valid UUID.");
        }

        return internalLogRepository.findById(logUUID).orElseThrow(
            () -> new InternalLogNotFoundException("InternalLog not found with ID: " + logId)
        );
    }

    public List<InternalLog> getFirstLogs() {
        return internalLogRepository.findFirst100Results();
    }

    public Optional<InternalLog> getInternalLog(Exception exception) {
        log.info("Generating an InternalLog instance");

        if (exception == null) {
            log.error("InternalLog NOT created due to null Exception");
            return Optional.empty();
        }

        if (exception.getClass() == null || exception.getClass().getSimpleName() == null) {
            log.error("getClass or getClass().getSimpleName() returned null");
            return Optional.empty();
        }

        InternalLog internalLog = new InternalLog();
        internalLog.setSimpleClassName(exception.getClass().getSimpleName());

        // Limitar Mensagem para 300 caracteres. O limite no banco é 400, mas vou deixar em 300 aqui.
        int errorMessageMaxLength = 300;
        String errorMessage = exception.getMessage();

        if (errorMessage == null) {
            errorMessage = "No message provided";
        } else if (errorMessage.length() >= errorMessageMaxLength) {
            errorMessage = errorMessage.substring(0, errorMessageMaxLength) + "...";
        }

        // Limitar a Stack Trace para 1000 caracteres. É o suficiente.
        int stackTraceMaxLength = 1000;
        String stackTracing = Arrays.toString(exception.getStackTrace());

        if (stackTracing.length() >= stackTraceMaxLength) {
            stackTracing = stackTracing.substring(0, stackTraceMaxLength) + "...";
        }

        internalLog.setErrorMessage(errorMessage);
        internalLog.setStackTrace(stackTracing);
        
        // Parte que envolve chamada ao BD. Pode lançar algum erro
        try {
            log.info("Saving InternalLog..");
            InternalLog savedInternalLog = internalLogRepository.save(internalLog);
            log.info("Saved!");
            return Optional.of(savedInternalLog);
        } catch (Exception errorToSave) {
            log.error("Unable to save the InternalLog!", errorToSave);
            return Optional.empty();
        }
    }

    public String unhandledExceptionReason(Optional<InternalLog> savedInternalLog) {
        StringBuilder messageBuilder = new StringBuilder();

        if (savedInternalLog.isEmpty()) {
            messageBuilder.append("An unhandled exception occurred while processing your request.")
                    .append(" Please try again later. The server also attempted to save the error logs,")
                    .append(" but this process also failed.");
        } else {
            messageBuilder.append("An unhandled exception occurred while processing your request.")
                    .append(" Please try again later or contact the system administrator")
                    .append(" informing the ID: " + savedInternalLog.get().getId())
                    .append(" to obtain support.");
        }

        return messageBuilder.toString();
    }

}
