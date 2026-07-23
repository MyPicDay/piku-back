package com.pikume.back.character.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CharacterErrorCode {
	CATALOG_UNAVAILABLE("캐릭터 목록을 불러올 수 없습니다.");

	private final String message;
}
