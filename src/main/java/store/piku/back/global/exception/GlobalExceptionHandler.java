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

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex, HttpServletRequest request) throws IOException {
        String message = ex.getMessage();

        if (message != null && isConnectionReset(message)) {
            // 클라이언트가 스트림 중간에 연결을 끊은 케이스
            log.debug("스트림 중단: 클라이언트 연결 끊김 - {} {}", request.getMethod(), request.getRequestURI());
            return ResponseEntity.noContent().build();
        }

        // 그 외 IOException은 다시 던져서 기본 처리
        discordWebhookService.ifPresent(service -> service.sendExceptionNotification(ex, request));
        log.error("IOException occurred: {}", message, ex);
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "파일 처리 중 오류가 발생했습니다."),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private boolean isConnectionReset(String message) {
        // OS나 JDK에 따라 메시지가 다를 수 있으므로 유사 패턴 포함
        return message.contains("Connection reset by peer")
                || message.contains("Broken pipe")
                || message.contains("An existing connection was forcibly closed");
    }

}