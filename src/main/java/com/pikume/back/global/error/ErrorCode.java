package com.pikume.back.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    DIARY_NOT_FOUND(404, "해당 일기 목록이 존재하지 않습니다."),
    DIARY_ACCESS_DENIED(403, "해당 일기에 대한 권한이 없습니다."),
    INVALID_FEED_CURSOR(400, "유효하지 않은 피드 커서입니다."),

    USER_NOT_FOUND(400, "존재하지 않는 사용자입니다."),

    INTERNAL_SERVER_ERROR(500, "서버에 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
