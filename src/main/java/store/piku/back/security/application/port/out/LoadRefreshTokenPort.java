package store.piku.back.security.application.port.out;

import store.piku.back.security.domain.RefreshToken;

import java.util.Optional;

public interface LoadRefreshTokenPort {

	Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
