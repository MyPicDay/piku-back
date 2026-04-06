package com.pikume.back.user.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum UserProblemType implements ApiProblemType {
	NICKNAME_CONFLICT("https://api.pikume.com/problems/user/nickname-conflict", HttpStatus.CONFLICT, "Conflict"),
	PROFILE_CONFLICT("https://api.pikume.com/problems/user/profile-conflict", HttpStatus.CONFLICT, "Conflict");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	UserProblemType(String type, HttpStatus status, String title) {
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
