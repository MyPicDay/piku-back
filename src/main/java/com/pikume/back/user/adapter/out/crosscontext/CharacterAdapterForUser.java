package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.user.application.port.out.LoadFixedCharacterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CharacterAdapterForUser implements LoadFixedCharacterPort {

	private final GetCharacterUseCase getCharacterUseCase;

	@Override
	public Optional<String> findFixedCharacterObjectKey(Long characterId) {
		return getCharacterUseCase.findFixedCharacterObjectKey(characterId);
	}
}
