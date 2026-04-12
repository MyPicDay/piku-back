package com.pikume.back.security.dto.response;

public record MobileTokenBundle(
		String tokenType,
		String accessToken,
		String refreshToken,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn
) {
}
