package com.pikume.back.user.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import com.pikume.back.user.application.exception.UserErrorCode;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum UserProblemType implements ApiProblemType {
	NOT_FOUND("https://api.pikume.com/problems/user/not-found", HttpStatus.NOT_FOUND, "Not Found"),
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

	public static UserProblemType from(UserErrorCode errorCode) {
		return switch (errorCode) {
			case USER_NOT_FOUND -> NOT_FOUND;
		};
	}
}
