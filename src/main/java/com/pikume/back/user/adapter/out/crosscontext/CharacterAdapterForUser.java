package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.character.application.dto.CharacterImageReferenceQuery;
import com.pikume.back.character.application.port.in.QueryCharacterImageReferencesUseCase;
import com.pikume.back.user.application.dto.AvatarCharacterReference;
import com.pikume.back.user.application.dto.AvatarCharacterSelection;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.out.ResolveAvatarCharacterReferencesPort;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;
import com.pikume.back.user.application.port.out.ResolveFixedCharacterAvatarPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CharacterAdapterForUser
		implements ResolveFixedCharacterAvatarPort, ResolveAvatarCharacterReferencesPort {

	private final GetCharacterUseCase getCharacterUseCase;
	private final QueryCharacterImageReferencesUseCase queryCharacterImageReferencesUseCase;

	@Override
	public Optional<String> resolveFixedCharacterObjectKey(Long characterId) {
		return getCharacterUseCase.findFixedCharacterObjectKey(characterId);
	}

	@Override
	public List<AvatarCharacterReference> resolveAvatarCharacterReferences(
			Collection<AvatarCharacterSelection> selections) {
		List<CharacterImageReferenceQuery> queries = selections.stream()
				.map(selection -> new CharacterImageReferenceQuery(
						selection.userId(), selection.characterId()))
				.toList();
		return queryCharacterImageReferencesUseCase.queryCharacterImageReferences(queries)
				.stream()
				.map(reference -> new AvatarCharacterReference(
						reference.userId(),
						reference.characterId(),
						new UserAvatarReference(
								reference.imageReference(),
								reference.absoluteUrl(),
								reference.publiclyAccessible())))
				.toList();
	}
}
