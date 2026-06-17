package com.pikume.back.admin.adapter.in.web.dto.request;

public record AdminLoginRequest(
		String loginId,
		String password
) {
}
