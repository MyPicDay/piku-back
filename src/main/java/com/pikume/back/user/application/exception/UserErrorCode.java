package com.pikume.back.user.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode {
	USER_NOT_FOUND("존재하지 않는 사용자입니다.");

	private final String message;
}
