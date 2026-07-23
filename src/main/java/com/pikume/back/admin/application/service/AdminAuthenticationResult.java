package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.dto.AdminSessionCredentialResult;
import com.pikume.back.admin.domain.AdminRole;

public record AdminAuthenticationResult(
		AdminSessionCredentialResult credentials,
		String nickname,
		AdminRole role
) {
}
