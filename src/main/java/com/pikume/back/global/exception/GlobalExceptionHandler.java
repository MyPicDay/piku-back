package com.pikume.back.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import com.pikume.back.global.error.ApiProblemType;
import com.pikume.back.global.error.CommonProblemType;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.error.ValidationProblemType;
import com.pikume.back.global.notification.DiscordWebhookService;
import com.pikume.back.global.util.RequestUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Optional<DiscordWebhookService> discordWebhookService;
    private final ProblemDetailFactory problemDetailFactory;

    @Autowired
    public GlobalExceptionHandler(Optional<DiscordWebhookService> discordWebhookService,
            ProblemDetailFactory problemDetailFactory) {
        this.discordWebhookService = discordWebhookService;
        this.problemDetailFactory = problemDetailFactory;
    }

    public GlobalExceptionHandler(Optional<DiscordWebhookService> discordWebhookService) {
        this(discordWebhookService, new ProblemDetailFactory());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException e,
            HttpServletRequest request) {
        log.warn("Validation failed: {}", e.getMessage());

        Map<String, String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null
                                ? error.getDefaultMessage()
                                : "메시지가 null입니다."));
        log.info("프론트에서 보내지는 에러: {}", errors);

        return buildValidationProblem("요청 값이 올바르지 않습니다.", errors, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParams(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        Map<String, String> errors = Map.of(
                ex.getParameterName(),
                String.format("'%s' parameter of type '%s' is missing", ex.getParameterName(), ex.getParameterType()));
        return buildValidationProblem("요청 값이 올바르지 않습니다.", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException e,
            HttpServletRequest request) {
        log.warn("Constraint violation failed: {}", e.getMessage());

        Map<String, String> errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage));

        return buildValidationProblem("요청 값이 올바르지 않습니다.", errors, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidationException(HandlerMethodValidationException e,
            HttpServletRequest request) {
        log.warn("Validation failed for method parameters: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            String message = result.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            errors.put(parameterName, message);
        }

        return buildValidationProblem("요청 값이 올바르지 않습니다.", errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
            HttpServletRequest request) {
        log.warn("Malformed request body: {}", e.getMessage());
        return buildProblem(CommonProblemType.MALFORMED_REQUEST, "요청 본문을 해석할 수 없습니다.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException e,
            HttpServletRequest request) {
        log.warn("Resource not found at path: {}", e.getResourcePath());
        return buildProblem(CommonProblemType.RESOURCE_NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.", request);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ProblemDetail> handleIOException(IOException ex, HttpServletRequest request) {
        String message = ex.getMessage();

        if (message != null && isConnectionReset(message)) {
            // 클라이언트가 스트림 중간에 연결을 끊은 케이스
            log.debug("스트림 중단: 클라이언트 연결 끊김 - {} {}", request.getMethod(), request.getRequestURI());
            return ResponseEntity.noContent().build();
        }

        // 그 외 IOException은 다시 던져서 기본 처리
        discordWebhookService.ifPresent(service -> service.sendExceptionNotification(ex, request));
        log.error("IOException occurred: {}", message, ex);
        return buildProblem(CommonProblemType.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다.", request);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e, HttpServletRequest request) {
        log.error("비동기 요청을 사용할 수 없습니다. IP: {}, User-Agent: {}, API: {} {}, 원인: {}",
                RequestUtil.getClientIp(request),
                request.getHeader("User-Agent"),
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage());
    }

    private boolean isConnectionReset(String message) {
        // OS나 JDK에 따라 메시지가 다를 수 있으므로 유사 패턴 포함
        return message.contains("Connection reset by peer")
                || message.contains("Broken pipe")
                || message.contains("An existing connection was forcibly closed");
    }

    private ResponseEntity<ProblemDetail> buildProblem(ApiProblemType problemType, String detail,
            HttpServletRequest request) {
        ProblemDetail problemDetail = problemDetailFactory.create(problemType, detail, request.getRequestURI());
        return ResponseEntity.status(problemType.status()).body(problemDetail);
    }

    private ResponseEntity<ProblemDetail> buildValidationProblem(String detail, Map<String, String> fieldErrors,
            HttpServletRequest request) {
        ProblemDetail problemDetail = problemDetailFactory.validation(detail, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(ValidationProblemType.INVALID_REQUEST.status()).body(problemDetail);
    }

}
