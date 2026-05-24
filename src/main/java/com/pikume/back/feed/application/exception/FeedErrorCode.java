package com.pikume.back.feed.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode {
	DIARY_NOT_FOUND("일기를 찾을 수 없습니다."),
	INVALID_CURSOR("유효하지 않은 피드 커서입니다."),
	INVALID_SORT("유효하지 않은 피드 정렬 값입니다.");

	private final String message;
}
