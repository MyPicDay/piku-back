package com.pikume.back.user.application.service;

import com.pikume.back.user.application.dto.AvatarCharacterReference;
import com.pikume.back.user.application.dto.AvatarCharacterSelection;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.exception.UserAvatarReferenceIntegrityException;
import com.pikume.back.user.application.port.out.ResolveAvatarCharacterReferencesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAvatarReferenceResolver {

	private final ResolveAvatarCharacterReferencesPort resolveAvatarCharacterReferencesPort;

	public Map<AvatarCharacterSelection, UserAvatarReference> resolveRequired(
			Collection<AvatarCharacterSelection> selections) {
		Set<AvatarCharacterSelection> requestedSelections = distinctSelections(selections);
		if (requestedSelections.isEmpty()) {
			return Map.of();
		}

		var references = resolveAvatarCharacterReferencesPort
				.resolveAvatarCharacterReferences(requestedSelections);
		if (references == null) {
			throw integrityException(requestedSelections);
		}
		Map<AvatarCharacterSelection, UserAvatarReference> referencesBySelection = new LinkedHashMap<>();
		for (AvatarCharacterReference reference : references) {
			AvatarCharacterSelection selection = reference.selection();
			if (!requestedSelections.contains(selection)
					|| referencesBySelection.putIfAbsent(selection, reference.imageReference()) != null) {
				throw integrityException(requestedSelections);
			}
		}

		Set<AvatarCharacterSelection> missingSelections = requestedSelections.stream()
				.filter(selection -> !referencesBySelection.containsKey(selection))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (!missingSelections.isEmpty()) {
			throw integrityException(missingSelections);
		}
		return Collections.unmodifiableMap(referencesBySelection);
	}

	private Set<AvatarCharacterSelection> distinctSelections(
			Collection<AvatarCharacterSelection> selections) {
		if (selections == null || selections.isEmpty()) {
			return Set.of();
		}
		if (selections.stream().anyMatch(java.util.Objects::isNull)) {
			throw new UserAvatarReferenceIntegrityException(List.of());
		}
		return selections.stream()
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private UserAvatarReferenceIntegrityException integrityException(
			Collection<AvatarCharacterSelection> selections) {
		return new UserAvatarReferenceIntegrityException(selections.stream()
				.map(AvatarCharacterSelection::characterId)
				.toList());
	}
}
