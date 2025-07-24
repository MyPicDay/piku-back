package store.piku.back.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "가입되지 않은 이메일입니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFICATION_NOT_FOUND", "인증 요청을 찾을 수 없습니다."),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."),

    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "CODE_EXPIRED", "인증 코드가 만료되었습니다."),
    CODE_MISMATCH(HttpStatus.BAD_REQUEST, "CODE_MISMATCH", "인증 코드가 일치하지 않습니다."),

    EMAIL_SEND_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILURE", "이메일 발송에 실패했습니다.");

    private final HttpStatus status;
    private final String errorCode;
    private final String message;
}

