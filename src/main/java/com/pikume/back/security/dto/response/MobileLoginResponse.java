package com.pikume.back.security.dto.response;

import com.pikume.back.security.dto.UserInfo;

public record MobileLoginResponse(
		String message,
		UserInfo user,
		MobileTokenBundle tokens
) {
}
