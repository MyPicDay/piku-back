package com.pikume.back.security.jwt;

public enum SecurityTokenType {
	USER_ACCESS,
	UNKNOWN;

	public static SecurityTokenType fromClaim(Object value) {
		if (value == null) {
			return USER_ACCESS;
		}
		try {
			return SecurityTokenType.valueOf(String.valueOf(value));
		} catch (IllegalArgumentException exception) {
			return UNKNOWN;
		}
	}
}
