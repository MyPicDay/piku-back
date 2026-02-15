package store.piku.back.character.application.port.in;

import store.piku.back.character.domain.Character;

import java.util.List;

/**
 * 캐릭터 조회 유스케이스 (Inbound Port)
 */
public interface GetCharacterUseCase {

	/**
	 * 고정 캐릭터 목록을 조회합니다.
	 */
	List<Character> getFixedCharacters();

	/**
	 * ID로 캐릭터를 조회합니다.
	 */
	Character getCharacterById(Long id);

	/**
	 * 고정 캐릭터 이미지 URL을 조회합니다.
	 */
	String getFixedCharacterImageUrl(Long characterId);

	/**
	 * 고정 캐릭터 이미지가 존재하는지 확인합니다.
	 */
	boolean isCharacterFixedImageExists(Long characterId);
}
