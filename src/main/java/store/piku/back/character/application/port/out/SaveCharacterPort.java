package store.piku.back.character.application.port.out;

import store.piku.back.character.domain.Character;

/**
 * 캐릭터 저장 Outbound Port
 */
public interface SaveCharacterPort {

	Character save(Character character);
}
