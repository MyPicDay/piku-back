package com.pikume.back.admin.adapter.in.web.dto.request;

public record SetAdminCredentialsRequest(
		String loginId,
		String password
) {
}
