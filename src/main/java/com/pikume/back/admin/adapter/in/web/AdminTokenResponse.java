package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.service.AdminTokenIssueResult;
import com.pikume.back.admin.domain.AdminRole;

public record AdminTokenResponse(
		String tokenType,
		String accessToken,
		long expiresInSeconds,
		long refreshTokenExpiresInSeconds,
		AdminPrincipalResponse admin
) {

	static AdminTokenResponse from(AdminTokenIssueResult result) {
		return new AdminTokenResponse(
				"Bearer",
				result.accessToken(),
				result.accessTokenExpiresInSeconds(),
				result.refreshTokenExpiresInSeconds(),
				new AdminPrincipalResponse(
						result.loginId(),
						result.nickname(),
						result.email(),
						result.role()));
	}

	public record AdminPrincipalResponse(
			String loginId,
			String nickname,
			String email,
			AdminRole role
	) {
	}
}
