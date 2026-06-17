package com.pikume.back.security.jwt;

public final class AdminAuthConstants {

	public static final String REFRESH_TOKEN_COOKIE_NAME = "admin_refresh_token";
	public static final long ACCESS_TOKEN_EXPIRATION_TIME = 1000L * 60 * 10;
	public static final long ONBOARDING_TOKEN_EXPIRATION_TIME = 1000L * 60 * 10;
	public static final long REFRESH_TOKEN_ABSOLUTE_EXPIRATION_TIME = 1000L * 60 * 60 * 8;
	public static final long REFRESH_TOKEN_IDLE_EXPIRATION_TIME = 1000L * 60 * 30;

	private AdminAuthConstants() {
	}
}
