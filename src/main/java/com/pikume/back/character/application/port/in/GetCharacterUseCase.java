package com.pikume.back.character.application.port.in;

import com.pikume.back.character.application.dto.CharacterResult;

import java.util.List;

/**
 * 캐릭터 조회 유스케이스 (Inbound Port)
 */
public interface GetCharacterUseCase {

	/**
	 * 고정 캐릭터 목록을 조회합니다. 응답의 imageUrl은 클라이언트 표시용 URL입니다.
	 */
	List<CharacterResult> getFixedCharacters();

	/**
	 * ID로 캐릭터를 조회합니다.
	 */
	CharacterResult getCharacterById(Long id);

	/**
	 * 고정 캐릭터 이미지 object key를 조회합니다.
	 */
	String getFixedCharacterObjectKey(Long characterId);

	/**
	 * 고정 캐릭터 이미지가 존재하는지 확인합니다.
	 */
	boolean isCharacterFixedImageExists(Long characterId);
}
