package com.pikume.back.character.adapter.out.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.character.application.exception.FixedCharacterImageNotFoundException;
import com.pikume.back.character.application.dto.CharacterImageContent;
import com.pikume.back.character.application.port.out.CharacterImageStoragePort;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.util.FileUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CharacterImageStorageAdapter implements CharacterImageStoragePort {

	private final FileUtil fileUtil;

	@Override
	public String saveCharacterImage(UploadedFileData file, CharacterCreationType type, String userId, String desiredName) {
		return fileUtil.saveCharacterImage(file, type, userId, desiredName);
	}

	@Override
	public CharacterImageContent loadCharacterImage(CharacterCreationType type, String userId, String fileName) {
		try {
			var resource = fileUtil.loadCharacterImageAsResource(type, userId, fileName);
			if (!resource.exists() || !resource.isReadable()) {
				throw new FixedCharacterImageNotFoundException(fileName);
			}
			try (var inputStream = resource.getInputStream()) {
				return new CharacterImageContent(fileName, fileUtil.getContentType(fileName), inputStream.readAllBytes());
			}
		} catch (IOException e) {
			throw new RuntimeException("캐릭터 이미지를 읽는 중 오류가 발생했습니다.", e);
		}
	}
}
