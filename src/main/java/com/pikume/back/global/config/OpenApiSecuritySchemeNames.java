package com.pikume.back.global.config;

public record OpenApiSecuritySchemeNames(
		String adminSessionCookie,
		String adminCsrfHeader
) {
}
