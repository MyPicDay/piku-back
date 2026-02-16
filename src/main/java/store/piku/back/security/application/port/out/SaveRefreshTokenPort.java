package store.piku.back.security.application.port.out;

import store.piku.back.security.domain.RefreshToken;

public interface SaveRefreshTokenPort {

	RefreshToken save(RefreshToken refreshToken);
}
