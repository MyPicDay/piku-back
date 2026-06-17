package com.pikume.back.admin.adapter.in.web.dto.request;

public record ChangeAdminPasswordRequest(
		String currentPassword,
		String newPassword
) {
}
