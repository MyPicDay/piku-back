package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.service.AdminAuthenticationResult;
import com.pikume.back.admin.domain.AdminRole;

public record AdminAuthenticationResponse(String nickname, AdminRole role) {

	static AdminAuthenticationResponse from(AdminAuthenticationResult result) {
		return new AdminAuthenticationResponse(result.nickname(), result.role());
	}
}
