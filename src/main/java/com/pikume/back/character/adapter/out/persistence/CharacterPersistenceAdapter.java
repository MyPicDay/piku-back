package com.pikume.back.character.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.character.application.port.out.LoadCharacterPort;
import com.pikume.back.character.application.port.out.SaveCharacterPort;
import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.vo.CharacterCreationType;

import java.util.List;
import java.util.Optional;

/**
 * Character Persistence Adapter
 * LoadCharacterPort, SaveCharacterPort를 구현합니다.
 */
@Component
@RequiredArgsConstructor
public class CharacterPersistenceAdapter implements LoadCharacterPort, SaveCharacterPort {

	private final CharacterJpaRepository characterJpaRepository;

	@Override
	public Optional<Character> findById(Long id) {
		return characterJpaRepository.findById(id);
	}

	@Override
	public List<Character> findByType(CharacterCreationType type) {
		return characterJpaRepository.findByType(type);
	}

	@Override
	public Character save(Character character) {
		return characterJpaRepository.save(character);
	}
}
