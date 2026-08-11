package com.pikume.back.creative.application.port.out;

import java.util.Optional;

public interface LoadSelectedCharacterReferencePort {

	Optional<String> loadSelectedCharacterReference(String userId, Long characterId);
}
