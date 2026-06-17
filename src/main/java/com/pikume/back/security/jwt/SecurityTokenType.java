package com.pikume.back.security.jwt;

public enum SecurityTokenType {
	USER_ACCESS,
	ADMIN_ACCESS,
	ADMIN_REFRESH,
	ADMIN_ONBOARDING,
	ADMIN_OTP_CHALLENGE,
	UNKNOWN;

	public static SecurityTokenType fromClaim(Object value) {
		if (value == null) {
			return USER_ACCESS;
		}
		try {
			return SecurityTokenType.valueOf(String.valueOf(value));
		} catch (IllegalArgumentException e) {
			return UNKNOWN;
		}
	}

	public boolean isAdminScoped() {
		return this == ADMIN_ACCESS
				|| this == ADMIN_REFRESH
				|| this == ADMIN_ONBOARDING
				|| this == ADMIN_OTP_CHALLENGE;
	}
}
