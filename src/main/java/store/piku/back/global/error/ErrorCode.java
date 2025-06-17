package store.piku.back.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    DIARY_NOT_FOUND(404, "해당 일기 목록이 존재하지 않습니다.");

    private final int code;
    private final String message;
}
