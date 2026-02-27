package com.pikume.back.security.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.security.application.port.out.DeleteRefreshTokenPort;
import com.pikume.back.security.application.port.out.LoadRefreshTokenPort;
import com.pikume.back.security.application.port.out.SaveRefreshTokenPort;
import com.pikume.back.security.domain.RefreshToken;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter
		implements LoadRefreshTokenPort, SaveRefreshTokenPort, DeleteRefreshTokenPort {

	private final RefreshTokenJpaRepository refreshTokenJpaRepository;

	@Override
	public Optional<RefreshToken> findByRefreshToken(String refreshToken) {
		return refreshTokenJpaRepository.findByRefreshToken(refreshToken);
	}

	@Override
	public RefreshToken save(RefreshToken refreshToken) {
		return refreshTokenJpaRepository.save(refreshToken);
	}

	@Override
	public void deleteByRefreshToken(String refreshToken) {
		refreshTokenJpaRepository.deleteByRefreshToken(refreshToken);
	}

	@Override
	public void deleteById(String key) {
		refreshTokenJpaRepository.deleteById(key);
	}
}
