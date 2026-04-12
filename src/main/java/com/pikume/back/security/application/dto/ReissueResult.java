package com.pikume.back.security.application.dto;

public record ReissueResult(
		String accessToken,
		String refreshToken,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn
) {
}
