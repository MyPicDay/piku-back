package com.pikume.back.user.auth.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum AuthProblemType implements ApiProblemType {
	USER_NOT_FOUND("https://api.pikume.com/problems/auth/user-not-found", HttpStatus.NOT_FOUND, "Not Found"),
	EMAIL_ALREADY_EXISTS("https://api.pikume.com/problems/auth/email-already-exists", HttpStatus.CONFLICT, "Conflict"),
	EMAIL_VERIFICATION_REQUIRED("https://api.pikume.com/problems/auth/email-verification-required", HttpStatus.FORBIDDEN, "Forbidden"),
	CODE_INVALID("https://api.pikume.com/problems/auth/code-invalid", HttpStatus.BAD_REQUEST, "Bad Request"),
	EMAIL_SEND_FAILURE("https://api.pikume.com/problems/auth/email-send-failure", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	AuthProblemType(String type, HttpStatus status, String title) {
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
