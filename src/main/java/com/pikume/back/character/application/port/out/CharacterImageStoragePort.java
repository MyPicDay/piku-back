package com.pikume.back.character.application.port.out;

import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.dto.UploadedFileData;

public interface CharacterImageStoragePort {

	String saveCharacterImage(UploadedFileData file, CharacterCreationType type, String userId, String desiredName);
}
