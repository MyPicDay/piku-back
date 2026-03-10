package com.pikume.back.character.application.port.in;

import com.pikume.back.character.application.dto.CharacterImageContent;
import com.pikume.back.character.application.dto.CharacterResult;
import com.pikume.back.global.dto.UploadedFileData;

/**
 * 캐릭터 등록/관리 유스케이스 (Inbound Port)
 */
public interface ManageCharacterUseCase {

	/**
	 * AI 캐릭터를 생성합니다.
	 */
	CharacterResult createAiCharacter(String userId, UploadedFileData imageFile, String desiredName);

	/**
	 * 캐릭터를 저장합니다.
	 */
	CharacterResult saveFixedCharacter(String imageUrl);

	/**
	 * 고정 캐릭터 이미지를 애플리케이션 이미지 콘텐츠로 로드합니다.
	 */
	CharacterImageContent getFixedCharacterImage(String fileName);
}
