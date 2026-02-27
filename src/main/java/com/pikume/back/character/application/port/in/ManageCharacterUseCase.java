package com.pikume.back.character.application.port.in;

import com.pikume.back.character.domain.Character;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 캐릭터 등록/관리 유스케이스 (Inbound Port)
 */
public interface ManageCharacterUseCase {

	/**
	 * AI 캐릭터를 생성합니다.
	 */
	Character createAiCharacter(String userId, MultipartFile imageFile, String desiredName);

	/**
	 * 캐릭터를 저장합니다.
	 */
	Character saveCharacter(Character character);

	/**
	 * 고정 캐릭터 이미지를 Resource로 로드합니다.
	 */
	Resource getFixedCharacterImageAsResource(String fileName);
}
