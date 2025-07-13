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

}