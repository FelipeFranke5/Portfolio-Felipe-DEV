package dev.franke.felipe.website_backend.exception;

import dev.franke.felipe.website_backend.dto.UnprocessableEntityResponse;
import dev.franke.felipe.website_backend.model.InternalLog;
import dev.franke.felipe.website_backend.service.InternalLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalBehaviourExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE =
            "Internal Server Error";

    private static final String VALIDATION_ERROR_MESSAGE =
            "The request has failed due to incorrect information in the payload provided.";

    private static final String PROJECT_EXCEPTION_ERROR_MESSAGE =
            "The request has failed due to incorrect use of PROJECT endpoint(s). Please " +
            "review the details in the response body to correct your request.";

    private static final String PROJECT_NOT_FOUND_EXCEPTION_ERROR_MESSAGE =
            "Unable to find the project you attempted to retrieve. Returning 404";

    private static final String SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE =
            "Unable to find the skill you attempted to retrieve. Returning 404";

    private static final String SKILL_EXCEPTION_ERROR_MESSAGE =
            "The request has failed due to incorrect use of SKILL endpoint(s). Please " +
            "review the details in the response body to correct your request.";

    private static final String INTERNAL_LOG_NOT_FOUND_EXCEPTION_ERROR_MESSAGE =
            "Unable to find InternalLog you attempted to retrieve. Returning 404";

    private static final String INTERNAL_LOG_EXCEPTION_ERROR_MESSAGE =
            "The request has failed due to incorrect use of INTERNAL_LOG endpoint(s). Please " +
            "review the details in the response body to correct your request.";

    private final InternalLogService internalLogService;

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleUnhandledException(Exception exception) {
        /*
            Mensagens genéricas
            Vamos salvar uma versão no BD que contenha algumas informações
            sobre o erro lançado. Ao solicitar o ID da mensagem,
            posso verificar o que ocorreu. Tipo um SysLog (bem simplista)
         */
        Optional<InternalLog> savedInternalLog = internalLogService.getInternalLog(exception);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", GENERIC_ERROR_MESSAGE);
        responseMap.put("reason", internalLogService.unhandledExceptionReason(savedInternalLog));

        // Antes de retornar, não podemos esquecer de adicionar um log para a própria aplicação
        log.error(
                "Unhandled Exception caught in " +
                        "handleUnhandledException method of GlobalBehaviourExceptionHandler", exception
        );
        return responseMap;
    }

    @ExceptionHandler(SkillException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleSkillException(SkillException exception) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", SKILL_EXCEPTION_ERROR_MESSAGE);
        responseMap.put("reason", exception.getMessage());
        return responseMap;
    }

    @ExceptionHandler(ProjectException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleProjectException(ProjectException exception) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", PROJECT_EXCEPTION_ERROR_MESSAGE);
        responseMap.put("reason", exception.getMessage());
        return responseMap;
    }

    @ExceptionHandler(InternalLogException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInternalLogException(InternalLogException exception) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", INTERNAL_LOG_EXCEPTION_ERROR_MESSAGE);
        responseMap.put("reason", exception.getMessage());
        return responseMap;
    }

    @ExceptionHandler(SkillNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleSkillNotFoundException(SkillNotFoundException exception) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", SKILL_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        responseMap.put("reason", exception.getMessage());
        return responseMap;
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleProjectNotFoundException(ProjectNotFoundException exception) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", PROJECT_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        responseMap.put("reason", exception.getMessage());
        return responseMap;
    }

    @ExceptionHandler(InternalLogNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleLogNotFoundException(InternalLogNotFoundException exception) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", INTERNAL_LOG_NOT_FOUND_EXCEPTION_ERROR_MESSAGE);
        responseMap.put("reason", exception.getMessage());
        return responseMap;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public UnprocessableEntityResponse handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        Map<String, List<String>> groupedErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        LinkedHashMap::new,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));
        return new UnprocessableEntityResponse(VALIDATION_ERROR_MESSAGE, toErrorList(groupedErrors));
    }

    private List<Map<String, List<String>>> toErrorList(Map<String, List<String>> grouped) {
        return grouped.entrySet().stream()
                .map(entry -> Map.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
