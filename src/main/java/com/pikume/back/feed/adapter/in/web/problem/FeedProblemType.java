package com.pikume.back.feed.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum FeedProblemType implements ApiProblemType {
	DIARY_NOT_FOUND("https://api.pikume.com/problems/feed/diary-not-found", HttpStatus.NOT_FOUND, "Not Found");

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
}
