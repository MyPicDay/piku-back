package store.piku.back.user.adapter.out.character;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.character.service.CharacterService;
import store.piku.back.user.application.port.out.LoadCharacterPort;

/**
 * 캐릭터 도메인 어댑터
 * User 도메인에서 캐릭터 정보를 조회하기 위한 Anti-Corruption Layer입니다.
 */
@Component
@RequiredArgsConstructor
public class CharacterAdapterForUser implements LoadCharacterPort {

	private final CharacterService characterService;

	@Override
	public String getFixedCharacterImageUrl(Long characterId) {
		return characterService.getFixedCharacterImageUrl(characterId);
	}

	@Override
	public boolean existsById(Long characterId) {
		return characterService.isCharacterFixedImageExists(characterId);
	}
}
