package com.pikume.back.character.domain.exception;

/**
 * 캐릭터를 찾을 수 없을 때 발생하는 도메인 예외
 */
public class CharacterNotFoundException extends RuntimeException {

	public CharacterNotFoundException(Long id) {
		super("Character not found with id: " + id);
	}

	public CharacterNotFoundException(String message) {
		super(message);
	}
}
