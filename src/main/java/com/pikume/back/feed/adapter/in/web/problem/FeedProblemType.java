package com.pikume.back.feed.adapter.in.web.problem;

import com.pikume.back.feed.application.exception.FeedErrorCode;
import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum FeedProblemType implements ApiProblemType {
	DIARY_NOT_FOUND("https://api.pikume.com/problems/feed/diary-not-found", HttpStatus.NOT_FOUND, "Not Found"),
	INVALID_CURSOR("https://api.pikume.com/problems/feed/invalid-cursor", HttpStatus.BAD_REQUEST, "Bad Request"),
	INVALID_SORT("https://api.pikume.com/problems/feed/invalid-sort", HttpStatus.BAD_REQUEST, "Bad Request");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	FeedProblemType(String type, HttpStatus status, String title) {
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

	public static FeedProblemType from(FeedErrorCode errorCode) {
		return switch (errorCode) {
			case DIARY_NOT_FOUND -> DIARY_NOT_FOUND;
			case INVALID_CURSOR -> INVALID_CURSOR;
			case INVALID_SORT -> INVALID_SORT;
		};
	}
}
