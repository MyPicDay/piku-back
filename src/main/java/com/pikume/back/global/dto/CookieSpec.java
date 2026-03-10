package com.pikume.back.global.dto;

public record CookieSpec(
		String name,
		String value,
		boolean httpOnly,
		boolean secure,
		String path,
		long maxAgeSeconds,
		String sameSite
) {
}
