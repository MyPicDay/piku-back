package com.pikume.back.diary.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum DiaryProblemType implements ApiProblemType {
	NOT_FOUND("https://api.pikume.com/problems/diary/not-found", HttpStatus.NOT_FOUND, "Not Found"),
	FORBIDDEN("https://api.pikume.com/problems/diary/forbidden", HttpStatus.FORBIDDEN, "Forbidden"),
	CONFLICT("https://api.pikume.com/problems/diary/conflict", HttpStatus.CONFLICT, "Conflict");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	DiaryProblemType(String type, HttpStatus status, String title) {
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
