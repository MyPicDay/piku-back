package com.pikume.back.creative.application.port.in;

import com.pikume.back.creative.application.dto.CharacterReferenceImage;

import java.util.Optional;

public interface PrepareCharacterReferenceUseCase {

	Optional<CharacterReferenceImage> prepareCharacterReference(String userId, Long characterId);
}
