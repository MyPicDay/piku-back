package com.pikume.back.user.application.port.out;

import com.pikume.back.user.application.dto.AvatarCharacterReference;
import com.pikume.back.user.application.dto.AvatarCharacterSelection;

import java.util.Collection;
import java.util.List;

public interface ResolveAvatarCharacterReferencesPort {

	List<AvatarCharacterReference> resolveAvatarCharacterReferences(
			Collection<AvatarCharacterSelection> selections);
}
