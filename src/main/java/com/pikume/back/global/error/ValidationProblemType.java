package com.pikume.back.global.error;

import org.springframework.http.HttpStatus;

import java.net.URI;

public enum ValidationProblemType implements ApiProblemType {
	INVALID_REQUEST("https://api.pikume.com/problems/validation/invalid-request", HttpStatus.BAD_REQUEST, "Bad Request");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	ValidationProblemType(String type, HttpStatus status, String title) {
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
