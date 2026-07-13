package com.pikume.back.user.auth.application.dto;

public record ReissueSessionResult(
		String accessToken,
		String refreshToken,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn) {
}
