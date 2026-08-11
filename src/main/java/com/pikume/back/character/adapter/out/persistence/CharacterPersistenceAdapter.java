package com.pikume.back.character.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.character.application.port.out.LoadCharacterReferencePort;
import com.pikume.back.character.application.port.out.LoadFixedCharactersPort;
import com.pikume.back.character.application.port.out.RecordFixedCharacterPort;
import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.vo.CharacterCreationType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Character Persistence Adapter
 * LoadCharacterPort, SaveCharacterPort를 구현합니다.
 */
@Component
@RequiredArgsConstructor
public class CharacterPersistenceAdapter
		implements LoadCharacterReferencePort, LoadFixedCharactersPort, RecordFixedCharacterPort {

	private final CharacterJpaRepository characterJpaRepository;

	@Override
	public Optional<Character> loadCharacterReference(Long id) {
		return characterJpaRepository.findById(id);
	}

	@Override
	public List<Character> loadCharacterReferences(Set<Long> characterIds) {
		return characterJpaRepository.findAllById(characterIds);
	}

	@Override
	public List<Character> loadFixedCharacters() {
		return characterJpaRepository.findByType(CharacterCreationType.FIXED);
	}

	@Override
	public Character recordFixedCharacter(Character character) {
		return characterJpaRepository.save(character);
	}
}
