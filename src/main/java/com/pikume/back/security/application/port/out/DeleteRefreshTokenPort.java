package com.pikume.back.security.application.port.out;

public interface DeleteRefreshTokenPort {

	void deleteByRefreshToken(String refreshToken);

	void deleteById(String key);
}
