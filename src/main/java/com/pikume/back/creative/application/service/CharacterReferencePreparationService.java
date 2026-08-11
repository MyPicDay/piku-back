package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.dto.CharacterReferenceImage;
import com.pikume.back.creative.application.exception.CreativeErrorCode;
import com.pikume.back.creative.application.exception.CreativeException;
import com.pikume.back.creative.application.policy.CharacterReferencePolicy;
import com.pikume.back.creative.application.port.in.PrepareCharacterReferenceUseCase;
import com.pikume.back.creative.application.port.out.LoadReferenceImageObjectPort;
import com.pikume.back.creative.application.port.out.LoadSelectedCharacterReferencePort;
import com.pikume.back.creative.application.port.out.LoadUserAvatarReferencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterReferencePreparationService implements PrepareCharacterReferenceUseCase {

	private final LoadUserAvatarReferencePort loadUserAvatarReferencePort;
	private final LoadSelectedCharacterReferencePort loadSelectedCharacterReferencePort;
	private final LoadReferenceImageObjectPort loadReferenceImageObjectPort;
	private final CharacterReferencePolicy characterReferencePolicy;

	@Override
	public Optional<CharacterReferenceImage> prepareCharacterReference(String userId, Long characterId) {
		Optional<String> objectKey = characterId == null
				? loadUserAvatarObjectKey(userId)
				: Optional.of(loadSelectedCharacterObjectKey(userId, characterId));
		return objectKey.flatMap(this::loadReferenceImage);
	}

	private Optional<String> loadUserAvatarObjectKey(String userId) {
		Optional<String> storedReference = loadUserAvatarReferencePort.loadUserAvatarReference(userId);
		try {
			return storedReference
					.map(characterReferencePolicy::canonicalize)
					.filter(reference -> !reference.isBlank())
					.filter(reference -> !characterReferencePolicy.isAbsoluteUrl(reference));
		} catch (IllegalArgumentException exception) {
			log.warn("event=character_reference_prepare_failed outcome=skipped userId={} reason={}",
					userId,
					exception.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	private String loadSelectedCharacterObjectKey(String userId, Long characterId) {
		return loadSelectedCharacterReferencePort.loadSelectedCharacterReference(userId, characterId)
				.filter(reference -> !reference.isBlank())
				.filter(reference -> !characterReferencePolicy.isAbsoluteUrl(reference))
				.orElseThrow(() -> new CreativeException(
						CreativeErrorCode.SELECTED_CHARACTER_UNAVAILABLE));
	}

	private Optional<CharacterReferenceImage> loadReferenceImage(String objectKey) {
		byte[] imageBytes = loadReferenceImageObjectPort.loadReferenceImage(objectKey);
		if (imageBytes == null || imageBytes.length == 0) {
			return Optional.empty();
		}
		return Optional.of(new CharacterReferenceImage(
				objectKey,
				Base64.getEncoder().encodeToString(imageBytes)));
	}
}
