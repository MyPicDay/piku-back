package com.pikume.back.admin.application.exception;

import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum AdminProblem implements ApiProblemType {
	UNAUTHENTICATED("https://api.pikume.com/problems/admin/unauthenticated", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	FORBIDDEN("https://api.pikume.com/problems/admin/forbidden", HttpStatus.FORBIDDEN, "Forbidden"),
	INVALID_CREDENTIALS("https://api.pikume.com/problems/admin/invalid-credentials", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	TEMPORARY_CREDENTIAL_EXPIRED("https://api.pikume.com/problems/admin/temporary-credential-expired", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	ONBOARDING_TOKEN_INVALID("https://api.pikume.com/problems/admin/onboarding-token-invalid", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	OTP_CHALLENGE_TOKEN_INVALID("https://api.pikume.com/problems/admin/otp-challenge-token-invalid", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	INVALID_REFRESH_TOKEN("https://api.pikume.com/problems/admin/invalid-refresh-token", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	REFRESH_TOKEN_REUSED("https://api.pikume.com/problems/admin/refresh-token-reused", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	ACCOUNT_LOCKED("https://api.pikume.com/problems/admin/account-locked", HttpStatus.LOCKED, "Locked"),
	DUPLICATE_EMAIL("https://api.pikume.com/problems/admin/email-conflict", HttpStatus.CONFLICT, "Conflict"),
	DUPLICATE_LOGIN_ID("https://api.pikume.com/problems/admin/login-id-conflict", HttpStatus.CONFLICT, "Conflict"),
	INVALID_REQUEST("https://api.pikume.com/problems/admin/invalid-request", HttpStatus.BAD_REQUEST, "Bad Request"),
	OTP_VERIFICATION_FAILED("https://api.pikume.com/problems/admin/otp-verification-failed", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	OTP_BLOCKED("https://api.pikume.com/problems/admin/otp-blocked", HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	AdminProblem(String type, HttpStatus status, String title) {
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
}
