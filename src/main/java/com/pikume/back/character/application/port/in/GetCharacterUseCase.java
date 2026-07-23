package com.pikume.back.character.application.port.in;

import java.util.Optional;

/**
 * User 소비자가 전환되기 전까지 유지하는 고정 캐릭터 참조 호환 계약이다.
 */
public interface GetCharacterUseCase {

	Optional<String> findFixedCharacterObjectKey(Long characterId);
}
