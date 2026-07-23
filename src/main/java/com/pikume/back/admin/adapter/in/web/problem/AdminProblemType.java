package com.pikume.back.admin.adapter.in.web.problem;

import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum AdminProblemType implements ApiProblemType {
	UNAUTHENTICATED("https://api.pikume.com/problems/admin/unauthenticated", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	FORBIDDEN("https://api.pikume.com/problems/admin/forbidden", HttpStatus.FORBIDDEN, "Forbidden"),
	CSRF_INVALID("https://api.pikume.com/problems/admin/csrf-invalid", HttpStatus.FORBIDDEN, "Forbidden"),
	NOT_FOUND("https://api.pikume.com/problems/admin/not-found", HttpStatus.NOT_FOUND, "Not Found"),
	INVALID_CREDENTIALS(
			"https://api.pikume.com/problems/admin/invalid-credentials",
			HttpStatus.UNAUTHORIZED,
			"Unauthorized"),
	TEMPORARY_CREDENTIAL_EXPIRED(
			"https://api.pikume.com/problems/admin/temporary-credential-expired",
			HttpStatus.UNAUTHORIZED,
			"Unauthorized"),
	ACCOUNT_LOCKED("https://api.pikume.com/problems/admin/account-locked", HttpStatus.LOCKED, "Locked"),
	DUPLICATE_EMAIL("https://api.pikume.com/problems/admin/email-conflict", HttpStatus.CONFLICT, "Conflict"),
	DUPLICATE_LOGIN_ID(
			"https://api.pikume.com/problems/admin/login-id-conflict",
			HttpStatus.CONFLICT,
			"Conflict"),
	INVALID_REQUEST("https://api.pikume.com/problems/admin/invalid-request", HttpStatus.BAD_REQUEST, "Bad Request"),
	OTP_VERIFICATION_FAILED(
			"https://api.pikume.com/problems/admin/otp-verification-failed",
			HttpStatus.UNAUTHORIZED,
			"Unauthorized"),
	OTP_BLOCKED(
			"https://api.pikume.com/problems/admin/otp-blocked",
			HttpStatus.TOO_MANY_REQUESTS,
			"Too Many Requests"),
	SESSION_STORE_UNAVAILABLE(
			"https://api.pikume.com/problems/admin/session-store-unavailable",
			HttpStatus.SERVICE_UNAVAILABLE,
			"Service Unavailable");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	AdminProblemType(String type, HttpStatus status, String title) {
		this.type = URI.create(type);
		this.status = status;
		this.title = title;
	}

	@Override
	public URI type() {
		return type;
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String title() {
		return title;
	}

	public static AdminProblemType from(AdminErrorCode errorCode) {
		return valueOf(errorCode.name());
	}
}
