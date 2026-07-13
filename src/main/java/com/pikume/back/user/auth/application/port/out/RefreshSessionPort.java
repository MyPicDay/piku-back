package com.pikume.back.user.auth.application.port.out;

import java.util.Optional;

public interface RefreshSessionPort {

	record RefreshSession(String key, String refreshToken, String userId) {
	}

	Optional<RefreshSession> loadSessionByRefreshToken(String refreshToken);
	void storeSession(RefreshSession session);
	void removeSessionByRefreshToken(String refreshToken);
	void removeSession(String key);
}
