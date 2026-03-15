package com.pikume.back.creative.application.port.out;

import com.pikume.back.creative.application.dto.CharacterReferenceImage;

import java.util.Optional;

public interface LoadCharacterReferencePort {

	Optional<CharacterReferenceImage> findByUserId(String userId);
}
