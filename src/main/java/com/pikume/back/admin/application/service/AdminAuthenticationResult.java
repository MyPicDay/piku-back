package com.pikume.back.admin.application.service;

import com.pikume.back.admin.domain.AdminRole;

public record AdminAuthenticationResult(
		AdminSessionCredentials credentials,
		String nickname,
		AdminRole role
) {
}
