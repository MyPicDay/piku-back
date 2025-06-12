package store.piku.back.user.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserExceptionMessage {
    USER_NOT_FOUND("사용자를 찾을 수 없습니다. ID: %s"),
    INVALID_USER_ID_FORMAT("잘못된 사용자 ID 형식입니다: %s"),
    USER_ALREADY_EXISTS("이미 존재하는 사용자입니다. 식별자: %s");

    private final String message;
} 