package store.piku.back.auth.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "store.piku.back.auth")
public class AuthExceptionHandler {

    public record ErrorResponse(String errorCode, String message) {}

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleCommentException(AuthException ex) {
        AuthErrorCode errorCode = ex.getErrorCode();
        ErrorResponse response = new ErrorResponse(errorCode.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }
}