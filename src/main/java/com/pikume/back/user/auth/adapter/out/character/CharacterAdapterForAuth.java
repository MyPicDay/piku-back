package com.pikume.back.user.auth.adapter.out.character;

import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.user.auth.application.port.out.LoadFixedCharacterForSignUpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CharacterAdapterForAuth implements LoadFixedCharacterForSignUpPort {

	private final GetCharacterUseCase getCharacterUseCase;

	@Override
	public Optional<String> findFixedCharacterObjectKey(Long characterId) {
		return getCharacterUseCase.findFixedCharacterObjectKey(characterId);
	}
}
