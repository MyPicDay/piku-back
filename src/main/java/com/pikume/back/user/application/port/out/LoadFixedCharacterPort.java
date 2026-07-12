package com.pikume.back.user.application.port.out;

import java.util.Optional;

public interface LoadFixedCharacterPort {

	Optional<String> findFixedCharacterObjectKey(Long characterId);
}
