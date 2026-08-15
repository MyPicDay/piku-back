package com.pikume.back.user.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode {
	USER_NOT_FOUND("존재하지 않는 사용자입니다."),
	AVATAR_CHARACTER_REFERENCE_INTEGRITY_VIOLATION("사용자 아바타 캐릭터 참조가 유효하지 않습니다.");

	private final String message;
}
