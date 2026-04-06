package com.pikume.back.security.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum SecurityProblemType implements ApiProblemType {
	UNAUTHENTICATED("https://api.pikume.com/problems/security/unauthenticated", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	INVALID_CREDENTIALS("https://api.pikume.com/problems/security/invalid-credentials", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	INVALID_REFRESH_TOKEN("https://api.pikume.com/problems/security/invalid-refresh-token", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	FORBIDDEN("https://api.pikume.com/problems/security/forbidden", HttpStatus.FORBIDDEN, "Forbidden");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	SecurityProblemType(String type, HttpStatus status, String title) {
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
