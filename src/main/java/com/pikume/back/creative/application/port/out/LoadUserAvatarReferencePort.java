package com.pikume.back.creative.application.port.out;

import java.util.Optional;

public interface LoadUserAvatarReferencePort {

	Optional<String> loadUserAvatarReference(String userId);
}
