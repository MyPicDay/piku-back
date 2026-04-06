package com.pikume.back.global.error;

import org.springframework.http.HttpStatus;

import java.net.URI;

public enum CommonProblemType implements ApiProblemType {
	RESOURCE_NOT_FOUND("https://api.pikume.com/problems/common/resource-not-found", HttpStatus.NOT_FOUND, "Not Found"),
	UNAUTHENTICATED("https://api.pikume.com/problems/common/unauthenticated", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	FORBIDDEN("https://api.pikume.com/problems/common/forbidden", HttpStatus.FORBIDDEN, "Forbidden"),
	CONFLICT("https://api.pikume.com/problems/common/conflict", HttpStatus.CONFLICT, "Conflict"),
	MALFORMED_REQUEST("https://api.pikume.com/problems/common/malformed-request", HttpStatus.BAD_REQUEST, "Bad Request"),
	UNPROCESSABLE_CONTENT("https://api.pikume.com/problems/common/unprocessable-content", HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Content"),
	RATE_LIMIT_EXCEEDED("https://api.pikume.com/problems/common/rate-limit-exceeded", HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests"),
	INTERNAL_SERVER_ERROR("https://api.pikume.com/problems/common/internal-server-error", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	CommonProblemType(String type, HttpStatus status, String title) {
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
