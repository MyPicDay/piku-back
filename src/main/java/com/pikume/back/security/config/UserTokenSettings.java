package com.pikume.back.security.config;

public final class UserTokenSettings {
	public static final long ACCESS_TOKEN_EXPIRATION_MILLIS = 1000L * 60 * 30;
	public static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 1000L * 60 * 60 * 24 * 7;
	private UserTokenSettings() { }
}
