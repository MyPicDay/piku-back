package com.pikume.back.character.adapter.out.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.character.application.port.out.CharacterImageStoragePort;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.util.FileUtil;

@Component
@RequiredArgsConstructor
public class CharacterImageStorageAdapter implements CharacterImageStoragePort {

	private final FileUtil fileUtil;

	@Override
	public String saveCharacterImage(UploadedFileData file, CharacterCreationType type, String userId, String desiredName) {
		return fileUtil.saveCharacterImage(file, type, userId, desiredName);
	}
}
