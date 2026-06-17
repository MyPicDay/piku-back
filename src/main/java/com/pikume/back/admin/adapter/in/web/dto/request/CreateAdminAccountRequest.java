package com.pikume.back.admin.adapter.in.web.dto.request;

import com.pikume.back.admin.domain.AdminRole;

public record CreateAdminAccountRequest(
		String email,
		String nickname,
		AdminRole role
) {
}
