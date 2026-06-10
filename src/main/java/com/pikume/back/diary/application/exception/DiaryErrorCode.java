package com.pikume.back.diary.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiaryErrorCode {
	DIARY_NOT_FOUND("해당 일기 목록이 존재하지 않습니다."),
	DIARY_ACCESS_DENIED("해당 일기에 대한 권한이 없습니다."),
	DIARY_INVALID_REQUEST("일기 요청 값이 올바르지 않습니다."),
	DIARY_IMAGE_RELOCATION_FAILED("일기 이미지 공개범위 변경에 실패했습니다."),
	DUPLICATE_DIARY("이미 해당 날짜에 일기가 존재합니다: %s");

	private final String message;

	public String format(Object... args) {
		return String.format(message, args);
	}
}
