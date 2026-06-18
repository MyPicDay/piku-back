package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.service.AdminAuthenticationResult;
import com.pikume.back.admin.domain.AdminRole;

public record AdminAuthenticationResponse(boolean authenticated, AdminProfile admin) {

	static AdminAuthenticationResponse from(AdminAuthenticationResult result) {
		return new AdminAuthenticationResponse(true, new AdminProfile(
				result.loginId(), result.nickname(), result.email(), result.role()));
	}

	public record AdminProfile(String loginId, String nickname, String email, AdminRole role) {
	}
}
