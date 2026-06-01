package com.pikume.back.user.application.port.out;

import java.util.Optional;

/**
 * 캐릭터 정보 조회 Outbound Port (타 도메인 의존성 추상화)
 */
public interface LoadCharacterPort {

	/**
	 * 캐릭터 ID로 고정 캐릭터 이미지 object key를 조회합니다.
	 *
	 * @param characterId 캐릭터 ID
	 * @return 고정 캐릭터 이미지 object key. 없거나 유효하지 않으면 Optional.empty()
	 */
	Optional<String> findFixedCharacterObjectKey(Long characterId);

	/**
	 * 캐릭터 ID로 캐릭터 존재 여부를 확인합니다.
	 *
	 * @param characterId 캐릭터 ID
	 * @return 존재 여부
	 */
	boolean existsById(Long characterId);
}
