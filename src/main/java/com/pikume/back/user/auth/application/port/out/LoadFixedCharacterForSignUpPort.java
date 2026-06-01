package com.pikume.back.user.auth.application.port.out;

import java.util.Optional;

public interface LoadFixedCharacterForSignUpPort {

	/**
	 * 회원가입에 사용할 고정 캐릭터 이미지 object key를 조회합니다.
	 */
	Optional<String> findFixedCharacterObjectKey(Long characterId);
}
