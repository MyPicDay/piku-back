package store.piku.back.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import store.piku.back.global.dto.ValidationErrorResponse;
import store.piku.back.global.error.ErrorCode;
import store.piku.back.global.error.ErrorResponse;
import store.piku.back.global.notification.DiscordWebhookService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Optional<DiscordWebhookService> discordWebhookService;

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncStreamFailure(AsyncRequestNotUsableException ex, HttpServletRequest request) {
        String message = ex.getMessage();

        if (message != null && message.startsWith("ServletOutputStream failed")) {
            log.debug("ServletOutputStream 전송 중 끊김 처리됨: {} {}", request.getMethod(), request.getRequestURI());
            return; // 예외 무시
        }

        // 그 외는 운영에 남기기 (선택)
        log.error("Unhandled AsyncRequestNotUsableException: {}", message, ex);
        discordWebhookService.ifPresent(service -> service.sendExceptionNotification(ex, request));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("BusinessException occurred: {}", e.getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = new ErrorResponse(errorCode.getStatus(), errorCode.getMessage());
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        // 필드별 에러 메시지 수집
        Map<String, String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null
                                ? error.getDefaultMessage()
                                : "메시지가 null입니다."
                ));
        log.info("프론트에서 보내지는 에러: {}", errors);

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled Exception occurred: {}", e.getMessage(), e);

        discordWebhookService.ifPresent(service -> service.sendExceptionNotification(e, request));

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(errorCode.getStatus(), errorCode.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 1. 클라이언트가 연결을 끊은 경우 (Broken pipe 등)
    @ExceptionHandler({AsyncRequestNotUsableException.class, IOException.class})
    public void handleClientDisconnected(Exception e, HttpServletRequest request) {
        if (isBrokenPipe(e)) {
            log.warn("Client disconnected before response was sent: {}", e.getMessage());
            // 응답하지 않음 (response는 더 이상 사용 불가)
        } else {
            discordWebhookService.ifPresent(service -> service.sendExceptionNotification(e, request));
            log.error("Unhandled IOException or async error", e);
        }
    }

    private boolean isBrokenPipe(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof IOException && cause.getMessage() != null &&
                    cause.getMessage().toLowerCase().contains("broken pipe")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

}