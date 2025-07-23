package store.piku.back.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    DIARY_NOT_FOUND(404, "해당 일기 목록이 존재하지 않습니다."),

    USER_NOT_FOUND(400, "존재하지 않는 사용자입니다."),

    INTERNAL_SERVER_ERROR(500, "서버에 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
