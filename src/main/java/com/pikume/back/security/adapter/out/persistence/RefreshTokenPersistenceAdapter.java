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
	public Optional<RefreshSession> findByRefreshToken(String refreshToken) {
		return repository.findByRefreshToken(refreshToken)
				.map(entity -> new RefreshSession(entity.getKey(), entity.getRefreshToken(), entity.getUserId()));
	}

	@Override
	public void save(RefreshSession session) {
		repository.save(new RefreshSessionEntity(session.key(), session.refreshToken(), session.userId()));
	}

	@Override
	public void deleteByRefreshToken(String refreshToken) { repository.deleteByRefreshToken(refreshToken); }

	@Override
	public void deleteByKey(String key) { repository.deleteById(key); }
}
