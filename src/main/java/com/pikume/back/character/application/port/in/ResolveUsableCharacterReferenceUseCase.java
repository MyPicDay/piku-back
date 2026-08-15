package com.pikume.back.character.application.port.in;

import com.pikume.back.character.application.dto.UsableCharacterReference;

import java.util.Optional;

public interface ResolveUsableCharacterReferenceUseCase {

	Optional<UsableCharacterReference> resolveUsableReference(String requestingUserId, Long characterId);
}
