package com.pikume.back.character.application.port.out;

import com.pikume.back.character.domain.Character;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LoadCharacterReferencePort {

	Optional<Character> loadCharacterReference(Long characterId);

	List<Character> loadCharacterReferences(Set<Long> characterIds);
}
