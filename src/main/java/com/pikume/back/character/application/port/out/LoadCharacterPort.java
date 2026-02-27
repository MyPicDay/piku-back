package com.pikume.back.character.application.port.out;

import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.vo.CharacterCreationType;

import java.util.List;
import java.util.Optional;

/**
 * 캐릭터 조회 Outbound Port
 */
public interface LoadCharacterPort {

	Optional<Character> findById(Long id);

	List<Character> findByType(CharacterCreationType type);
}
