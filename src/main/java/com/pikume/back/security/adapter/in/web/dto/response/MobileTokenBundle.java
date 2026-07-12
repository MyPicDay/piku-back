package com.pikume.back.security.adapter.in.web.dto.response;
public record MobileTokenBundle(String tokenType, String accessToken, String refreshToken,
		long accessTokenExpiresIn, long refreshTokenExpiresIn) { }
