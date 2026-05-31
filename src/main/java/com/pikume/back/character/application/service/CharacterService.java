package com.pikume.back.character.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.character.application.dto.CharacterResult;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.character.application.port.in.ManageCharacterUseCase;
import com.pikume.back.character.application.port.out.CharacterImageStoragePort;
import com.pikume.back.character.application.port.out.FixedCharacterAssetCatalogPort;
import com.pikume.back.character.application.port.out.LoadCharacterPort;
import com.pikume.back.character.application.port.out.SaveCharacterPort;
import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.exception.CharacterNotFoundException;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.port.out.ResolveImageUrlPort;
import com.pikume.back.global.util.CharacterAvatarPathNormalizer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
	private final FixedCharacterAssetCatalogPort fixedCharacterAssetCatalogPort;
	private final ResolveImageUrlPort resolveImageUrlPort;

	// === GetCharacterUseCase ===

	@Override
	public List<CharacterResult> getFixedCharacters() {
		return loadCharacterPort.findByType(CharacterCreationType.FIXED).stream()
				.map(this::toDisplayResult)
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
			return CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(character.get().getImageUrl());
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
	@Transactional
	public int synchronizeFixedCharactersFromStorageCatalog() {
		Set<String> dbObjectKeys = loadExistingFixedCharacterObjectKeysFromDb();
		List<String> storageObjectKeys = loadFixedCharacterObjectKeysFromStorage();
		if (storageObjectKeys.isEmpty()) {
			log.warn("MinIO fixed character catalog가 비어 있어 DB 동기화를 건너뜁니다.");
			return 0;
		}

		Set<String> storageObjectKeySet = Set.copyOf(storageObjectKeys);
		int newCharactersAdded = synchronizeStorageCharactersToDb(dbObjectKeys, storageObjectKeys);
		logUnexpectedCharacterRows(dbObjectKeys, storageObjectKeySet);
		return newCharactersAdded;
	}

	private Set<String> loadExistingFixedCharacterObjectKeysFromDb() {
		Set<String> dbObjectKeys = loadCharacterPort.findByType(CharacterCreationType.FIXED).stream()
				.map(Character::getImageUrl)
				.map(this::normalizeExistingFixedCharacterObjectKey)
				.flatMap(Optional::stream)
				.collect(Collectors.toSet());
		log.info("DB에 등록된 기존 고정 캐릭터 object key 수: {}", dbObjectKeys.size());
		return dbObjectKeys;
	}

	private Optional<String> normalizeExistingFixedCharacterObjectKey(String imageUrl) {
		try {
			String objectKey = CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(imageUrl);
			return objectKey.isBlank() ? Optional.empty() : Optional.of(objectKey);
		} catch (IllegalArgumentException e) {
			log.warn("잘못된 DB fixed character object key를 건너뜁니다. imageUrl={} reason={}",
					imageUrl,
					e.getMessage());
			return Optional.empty();
		}
	}

	private List<String> loadFixedCharacterObjectKeysFromStorage() {
		try {
			return fixedCharacterAssetCatalogPort.listFixedCharacterObjectKeys().stream()
					.map(this::normalizeFixedCharacterCatalogObjectKey)
					.flatMap(Optional::stream)
					.distinct()
					.toList();
		} catch (RuntimeException e) {
			log.warn("MinIO fixed character catalog 조회 실패. 기존 DB 값을 사용합니다. reason={}", e.getMessage());
			return List.of();
		}
	}

	private Optional<String> normalizeFixedCharacterCatalogObjectKey(String objectKey) {
		try {
			String normalizedObjectKey = CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(objectKey);
			return normalizedObjectKey.isBlank() ? Optional.empty() : Optional.of(normalizedObjectKey);
		} catch (IllegalArgumentException e) {
			log.warn("잘못된 MinIO fixed character object key를 건너뜁니다. objectKey={} reason={}",
					objectKey,
					e.getMessage());
			return Optional.empty();
		}
	}

	private int synchronizeStorageCharactersToDb(Set<String> dbObjectKeys, List<String> storageObjectKeys) {
		int newCharactersAdded = 0;
		for (String objectKey : storageObjectKeys) {
			if (!dbObjectKeys.contains(objectKey)) {
				saveCharacterPort.save(new Character(objectKey, CharacterCreationType.FIXED));
				log.info("새로운 고정 캐릭터 DB 추가: objectKey={}", objectKey);
				newCharactersAdded++;
			}
		}
		return newCharactersAdded;
	}

	private void logUnexpectedCharacterRows(Set<String> dbObjectKeys, Set<String> storageObjectKeys) {
		for (String dbObjectKey : dbObjectKeys) {
			if (!storageObjectKeys.contains(dbObjectKey)) {
				log.warn("DB에 등록된 고정 캐릭터 object key '{}'가 MinIO catalog에 없습니다.", dbObjectKey);
			}
		}
	}

	private CharacterResult toResult(Character character) {
		String imageUrl = character.getImageUrl();
		if (character.getType() == CharacterCreationType.FIXED) {
			imageUrl = CharacterAvatarPathNormalizer.normalizeFixedCharacterObjectKey(imageUrl);
		}
		return new CharacterResult(character.getId(), character.getUserId(), imageUrl, character.getType());
	}

	private CharacterResult toDisplayResult(Character character) {
		CharacterResult result = toResult(character);
		String imageUrl = result.imageUrl();
		if (imageUrl == null || imageUrl.isBlank() || CharacterAvatarPathNormalizer.isAbsoluteUrl(imageUrl)) {
			return result;
		}
		return new CharacterResult(
				result.id(),
				result.userId(),
				resolveImageUrlPort.getPhotoUrl(imageUrl, true),
				result.type());
	}
}
