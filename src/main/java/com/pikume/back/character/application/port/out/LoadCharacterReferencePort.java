package com.pikume.back.character.application.port.out;

import com.pikume.back.character.domain.Character;

import java.util.Optional;

public interface LoadCharacterReferencePort {

	Optional<Character> loadCharacterReference(Long characterId);
}
