package com.pikume.back.creative.adapter.out.crosscontext;

import com.pikume.back.character.application.dto.UsableCharacterReference;
import com.pikume.back.character.application.port.in.ResolveUsableCharacterReferenceUseCase;
import com.pikume.back.creative.application.port.out.LoadSelectedCharacterReferencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CharacterReferenceAdapterForCreative implements LoadSelectedCharacterReferencePort {

	private final ResolveUsableCharacterReferenceUseCase resolveUsableCharacterReferenceUseCase;

	@Override
	public Optional<String> loadSelectedCharacterReference(String userId, Long characterId) {
		return resolveUsableCharacterReferenceUseCase.resolveUsableReference(userId, characterId)
				.map(UsableCharacterReference::storageReference);
	}
}
