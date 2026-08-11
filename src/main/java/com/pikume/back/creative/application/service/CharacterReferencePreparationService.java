package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.dto.CharacterReferenceImage;
import com.pikume.back.creative.application.policy.CharacterReferencePolicy;
import com.pikume.back.creative.application.port.in.PrepareCharacterReferenceUseCase;
import com.pikume.back.creative.application.port.out.LoadReferenceImageObjectPort;
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
	private final LoadReferenceImageObjectPort loadReferenceImageObjectPort;
	private final CharacterReferencePolicy characterReferencePolicy;

	@Override
	public Optional<CharacterReferenceImage> prepareCharacterReference(String userId) {
		try {
			return loadUserAvatarReferencePort.loadUserAvatarReference(userId)
					.filter(reference -> !reference.isBlank())
					.filter(reference -> !characterReferencePolicy.isAbsoluteUrl(reference))
					.flatMap(this::loadReferenceImage);
		} catch (RuntimeException exception) {
			log.warn("event=character_reference_prepare_failed outcome=skipped userId={} reason={}",
					userId,
					exception.getClass().getSimpleName());
			return Optional.empty();
		}
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
