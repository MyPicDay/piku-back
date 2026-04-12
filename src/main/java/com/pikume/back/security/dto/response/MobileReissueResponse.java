package com.pikume.back.security.dto.response;

public record MobileReissueResponse(
		String message,
		MobileTokenBundle tokens
) {
}
