package com.pikume.back.user.application.port.out;

import java.util.Optional;

public interface ResolveFixedCharacterAvatarPort {

	Optional<String> resolveFixedCharacterObjectKey(Long characterId);
}
