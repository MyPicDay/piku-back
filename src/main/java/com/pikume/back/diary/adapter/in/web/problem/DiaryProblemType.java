package com.pikume.back.diary.adapter.in.web.problem;

import com.pikume.back.diary.application.exception.DiaryErrorCode;
import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum DiaryProblemType implements ApiProblemType {
	INVALID_REQUEST("https://api.pikume.com/problems/diary/invalid-request", HttpStatus.BAD_REQUEST, "Bad Request"),
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

	public static DiaryProblemType from(DiaryErrorCode errorCode) {
		return switch (errorCode) {
			case DIARY_INVALID_REQUEST -> INVALID_REQUEST;
			case DIARY_NOT_FOUND -> NOT_FOUND;
			case DIARY_ACCESS_DENIED -> FORBIDDEN;
			case DUPLICATE_DIARY -> CONFLICT;
		};
	}
}
