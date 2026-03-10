package com.pikume.back.character.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.character.application.dto.CharacterImageContent;
import com.pikume.back.character.application.dto.CharacterResult;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.character.application.port.in.ManageCharacterUseCase;
import com.pikume.back.character.application.port.out.CharacterImageStoragePort;
import com.pikume.back.character.application.port.out.LoadCharacterPort;
import com.pikume.back.character.application.port.out.SaveCharacterPort;
import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.exception.CharacterNotFoundException;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.dto.UploadedFileData;

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
	private final CharacterImageStoragePort characterImageStoragePort;

	// === GetCharacterUseCase ===

	@Override
	public List<CharacterResult> getFixedCharacters() {
		return loadCharacterPort.findByType(CharacterCreationType.FIXED).stream()
				.map(this::toResult)
				.toList();
	}

	@Override
	public CharacterResult getCharacterById(Long id) {
		Character character = loadCharacterPort.findById(id)
				.orElseThrow(() -> {
					log.error("Character not found with id: {}", id);
					return new CharacterNotFoundException(id);
				});
		return toResult(character);
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
	public CharacterResult createAiCharacter(String userId, UploadedFileData imageFile, String desiredName) {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("AI 캐릭터 생성을 위해서는 사용자 정보가 필요합니다.");
		}
		String savedFileName = characterImageStoragePort.saveCharacterImage(
				imageFile,
				CharacterCreationType.AI_GENERATED,
				userId,
				desiredName);
		Character newCharacter = new Character(userId, savedFileName);
		return toResult(saveCharacterPort.save(newCharacter));
	}

	@Override
	@Transactional
	public CharacterResult saveFixedCharacter(String imageUrl) {
		return toResult(saveCharacterPort.save(new Character(imageUrl, CharacterCreationType.FIXED)));
	}

	@Override
	public CharacterImageContent getFixedCharacterImage(String fileName) {
		return characterImageStoragePort.loadCharacterImage(CharacterCreationType.FIXED, null, fileName);
	}

	private CharacterResult toResult(Character character) {
		return new CharacterResult(character.getId(), character.getUserId(), character.getImageUrl(), character.getType());
	}
}
