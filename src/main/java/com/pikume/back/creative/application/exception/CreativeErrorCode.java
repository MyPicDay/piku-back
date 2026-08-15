package com.pikume.back.creative.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CreativeErrorCode {
	QUOTA_EXCEEDED("일일 생성 횟수를 모두 사용하셨습니다."),
	SELECTED_CHARACTER_UNAVAILABLE("선택한 캐릭터를 사용할 수 없습니다."),
	CHARACTER_REFERENCE_UNAVAILABLE("참조 캐릭터 이미지를 불러올 수 없습니다."),
	IMAGE_GENERATION_FAILED("AI 이미지 생성에 실패했습니다."),
	IMAGE_STORAGE_FAILED("생성 이미지를 저장할 수 없습니다."),
	GENERATION_NOT_FOUND("AI 이미지 생성 이력을 찾을 수 없습니다.");

	private final String message;
}
