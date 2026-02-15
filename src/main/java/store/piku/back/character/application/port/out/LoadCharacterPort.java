package store.piku.back.character.application.port.out;

import store.piku.back.character.domain.Character;
import store.piku.back.character.domain.vo.CharacterCreationType;

import java.util.List;
import java.util.Optional;

/**
 * 캐릭터 조회 Outbound Port
 */
public interface LoadCharacterPort {

	Optional<Character> findById(Long id);

	List<Character> findByType(CharacterCreationType type);
}
