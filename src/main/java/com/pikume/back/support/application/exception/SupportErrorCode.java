package com.pikume.back.support.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupportErrorCode {
	SUBMITTER_NOT_FOUND("문의 제출 사용자를 찾을 수 없습니다."),
	INVALID_INQUIRY("문의 요청을 처리할 수 없습니다."),
	ATTACHMENT_STORAGE_FAILED("문의 첨부 이미지를 저장할 수 없습니다.");

	private final String message;
}
