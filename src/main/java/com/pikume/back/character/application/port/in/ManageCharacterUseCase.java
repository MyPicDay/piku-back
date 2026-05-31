package com.pikume.back.character.application.port.in;

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
	 * MinIO fixed character catalog와 DB를 동기화합니다.
	 */
	int synchronizeFixedCharactersFromStorageCatalog();
}
