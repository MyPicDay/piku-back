package store.piku.back.comment.exception;

import lombok.Getter;

@Getter
public class CommentException extends RuntimeException {
    private final CommentErrorCode errorCode;

    public CommentException(CommentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CommentException(CommentErrorCode errorCode,  Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
