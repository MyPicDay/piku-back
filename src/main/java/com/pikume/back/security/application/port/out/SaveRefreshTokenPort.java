package com.pikume.back.security.application.port.out;

import com.pikume.back.security.domain.RefreshToken;

public interface SaveRefreshTokenPort {

	RefreshToken save(RefreshToken refreshToken);
}
