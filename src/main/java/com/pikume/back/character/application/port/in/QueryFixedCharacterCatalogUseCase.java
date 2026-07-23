package com.pikume.back.character.application.port.in;

import com.pikume.back.character.application.dto.CharacterResult;

import java.util.List;

public interface QueryFixedCharacterCatalogUseCase {

	List<CharacterResult> queryFixedCharacters();
}
