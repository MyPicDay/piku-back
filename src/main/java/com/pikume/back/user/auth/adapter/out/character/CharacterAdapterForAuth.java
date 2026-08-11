package com.pikume.back.user.auth.adapter.out.character;

import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.user.auth.application.port.out.CheckSignUpCharacterSelectionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CharacterAdapterForAuth implements CheckSignUpCharacterSelectionPort {

	private final GetCharacterUseCase getCharacterUseCase;

	@Override
	public boolean isSelectableFixedCharacter(Long characterId) {
		return getCharacterUseCase.findFixedCharacterObjectKey(characterId).isPresent();
	}
}
