package com.pikume.back.character.application.port.in;

import java.util.Optional;

public interface ResolveFixedCharacterReferenceUseCase {

	Optional<String> resolveFixedCharacterObjectKey(Long characterId);
}
