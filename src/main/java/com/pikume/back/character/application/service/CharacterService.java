package com.pikume.back.character.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.character.application.port.in.ManageCharacterUseCase;
import com.pikume.back.character.application.port.out.LoadCharacterPort;
import com.pikume.back.character.application.port.out.SaveCharacterPort;
import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.exception.CharacterNotFoundException;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.util.FileUtil;

import java.util.List;
import java.util.Optional;

import static com.pikume.back.global.util.FileConstants.CHARACTERS_BASE_DIR_NAME;
import static com.pikume.back.global.util.FileConstants.FIXED_CHARACTER_SUB_DIR_NAME;

/**
 * Character Application Service
 * GetCharacterUseCase, ManageCharacterUseCase를 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CharacterService implements GetCharacterUseCase, ManageCharacterUseCase {

	private final LoadCharacterPort loadCharacterPort;
	private final SaveCharacterPort saveCharacterPort;
	private final FileUtil fileUtil;

	// === GetCharacterUseCase ===

	@Override
	public List<Character> getFixedCharacters() {
		return loadCharacterPort.findByType(CharacterCreationType.FIXED);
	}

	@Override
	public Character getCharacterById(Long id) {
		return loadCharacterPort.findById(id)
				.orElseThrow(() -> {
					log.error("Character not found with id: {}", id);
					return new CharacterNotFoundException(id);
				});
	}

	@Override
	public String getFixedCharacterImageUrl(Long characterId) {
		Optional<Character> character = loadCharacterPort.findById(characterId);
		if (character.isPresent() && character.get().getType() == CharacterCreationType.FIXED) {
			return CHARACTERS_BASE_DIR_NAME + "/" + FIXED_CHARACTER_SUB_DIR_NAME + "/" + character.get().getImageUrl();
		} else {
			log.warn("고정 캐릭터 이미지가 존재하지 않거나 잘못된 캐릭터 ID입니다: {}", characterId);
			return null;
		}
	}

	@Override
	public boolean isCharacterFixedImageExists(Long characterId) {
		Optional<Character> character = loadCharacterPort.findById(characterId);
		return character.isPresent() && character.get().getType() == CharacterCreationType.FIXED;
	}

	// === ManageCharacterUseCase ===

	@Override
	@Transactional
	public Character createAiCharacter(String userId, MultipartFile imageFile, String desiredName) {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("AI 캐릭터 생성을 위해서는 사용자 정보가 필요합니다.");
		}
		String savedFileName = fileUtil.saveCharacterImage(imageFile, CharacterCreationType.AI_GENERATED, userId,
				desiredName);
		Character newCharacter = new Character(userId, savedFileName);
		return saveCharacterPort.save(newCharacter);
	}

	@Override
	@Transactional
	public Character saveCharacter(Character character) {
		return saveCharacterPort.save(character);
	}

	@Override
	public Resource getFixedCharacterImageAsResource(String fileName) {
		return fileUtil.loadCharacterImageAsResource(CharacterCreationType.FIXED, null, fileName);
	}
}
