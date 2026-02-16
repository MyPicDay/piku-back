package store.piku.back.security.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.piku.back.security.application.port.out.DeleteRefreshTokenPort;
import store.piku.back.security.application.port.out.LoadRefreshTokenPort;
import store.piku.back.security.application.port.out.SaveRefreshTokenPort;
import store.piku.back.security.domain.RefreshToken;

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
