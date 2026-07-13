package com.pikume.back.security.adapter.out.persistence;

import com.pikume.back.user.auth.application.port.out.RefreshSessionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshSessionPort {
	private final RefreshTokenJpaRepository repository;

	@Override
	public Optional<RefreshSession> loadSessionByRefreshToken(String refreshToken) {
		return repository.findByRefreshToken(refreshToken)
				.map(entity -> new RefreshSession(entity.getKey(), entity.getRefreshToken(), entity.getUserId()));
	}

	@Override
	public void storeSession(RefreshSession session) {
		repository.save(new RefreshSessionEntity(session.key(), session.refreshToken(), session.userId()));
	}

	@Override
	public void removeSessionByRefreshToken(String refreshToken) { repository.deleteByRefreshToken(refreshToken); }

	@Override
	public void removeSession(String key) { repository.deleteById(key); }
}
