package com.pikume.back.character.adapter.in.web.problem;

import com.pikume.back.character.application.exception.CharacterErrorCode;
import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum CharacterProblemType implements ApiProblemType {
	CATALOG_UNAVAILABLE(
			"https://api.pikume.com/problems/character/catalog-unavailable",
			HttpStatus.INTERNAL_SERVER_ERROR,
			"Internal Server Error");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	CharacterProblemType(String type, HttpStatus status, String title) {
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

	public static CharacterProblemType from(CharacterErrorCode errorCode) {
		return switch (errorCode) {
			case CATALOG_UNAVAILABLE -> CATALOG_UNAVAILABLE;
		};
	}
}
