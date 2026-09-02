package com.pikume.back.testsupport;

import com.pikume.back.character.adapter.out.persistence.CharacterJpaRepository;
import com.pikume.back.character.domain.Character;

public final class FixedCharacterTestFixture {

	private static final String IMAGE_REFERENCE = "public/characters/fixed/test-avatar.webp";

	private FixedCharacterTestFixture() {
	}

	public static Long save(CharacterJpaRepository characterJpaRepository) {
		return characterJpaRepository.save(Character.fixed(IMAGE_REFERENCE)).getId();
	}
}
