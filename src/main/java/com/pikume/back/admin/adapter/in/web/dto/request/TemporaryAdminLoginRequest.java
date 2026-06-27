package com.pikume.back.admin.adapter.in.web.dto.request;

public record TemporaryAdminLoginRequest(
		String email,
		String temporaryPassword
) {
}
