package com.pikume.back.character.application.port.out;

import com.pikume.back.character.domain.Character;

import java.util.List;

public interface LoadFixedCharactersPort {

	List<Character> loadFixedCharacters();
}
