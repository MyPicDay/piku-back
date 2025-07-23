package store.piku.back.comment.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "store.piku.back.comment")
public class CommentExceptionHandler {

    public record ErrorResponse(String errorCode, String message) {}

    @ExceptionHandler(CommentException.class)
    public ResponseEntity<ErrorResponse> handleCommentException(CommentException ex) {
        CommentErrorCode errorCode = ex.getErrorCode();
        ErrorResponse response = new ErrorResponse(errorCode.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

}