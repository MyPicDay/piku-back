package com.pikume.back.character.application.exception;

public class FixedCharacterImageNotFoundException extends RuntimeException {
	public FixedCharacterImageNotFoundException(String fileName) {
		super("고정 캐릭터 이미지를 찾을 수 없습니다: " + fileName);
	}
}
