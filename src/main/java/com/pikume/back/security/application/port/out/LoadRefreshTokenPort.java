package com.pikume.back.security.application.port.out;

import com.pikume.back.security.domain.RefreshToken;

import java.util.Optional;

public interface LoadRefreshTokenPort {

	Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
