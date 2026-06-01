package com.pikume.back.user.adapter.out.character;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.user.application.port.out.LoadCharacterPort;

import java.util.Optional;

/**
 * 캐릭터 도메인 어댑터
 * User 도메인에서 캐릭터 정보를 조회하기 위한 Anti-Corruption Layer입니다.
 */
@Component
@RequiredArgsConstructor
public class CharacterAdapterForUser implements LoadCharacterPort {

	private final GetCharacterUseCase getCharacterUseCase;

	@Override
	public Optional<String> findFixedCharacterObjectKey(Long characterId) {
		return getCharacterUseCase.findFixedCharacterObjectKey(characterId);
	}

	@Override
	public boolean existsById(Long characterId) {
		return getCharacterUseCase.isCharacterFixedImageExists(characterId);
	}
}
