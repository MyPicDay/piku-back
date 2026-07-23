package com.pikume.back.creative.adapter.in.web.problem;

import com.pikume.back.creative.application.exception.CreativeErrorCode;
import com.pikume.back.global.error.ApiProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum CreativeProblemType implements ApiProblemType {
	QUOTA_EXCEEDED(
			"https://api.pikume.com/problems/common/rate-limit-exceeded",
			HttpStatus.TOO_MANY_REQUESTS,
			"Too Many Requests"),
	CHARACTER_REFERENCE_UNAVAILABLE(
			"https://api.pikume.com/problems/creative/character-reference-unavailable",
			HttpStatus.INTERNAL_SERVER_ERROR,
			"Internal Server Error"),
	IMAGE_GENERATION_FAILED(
			"https://api.pikume.com/problems/creative/image-generation-failed",
			HttpStatus.INTERNAL_SERVER_ERROR,
			"Internal Server Error"),
	IMAGE_STORAGE_FAILED(
			"https://api.pikume.com/problems/creative/image-storage-failed",
			HttpStatus.INTERNAL_SERVER_ERROR,
			"Internal Server Error"),
	GENERATION_NOT_FOUND(
			"https://api.pikume.com/problems/creative/generation-not-found",
			HttpStatus.NOT_FOUND,
			"Not Found");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	CreativeProblemType(String type, HttpStatus status, String title) {
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

	public static CreativeProblemType from(CreativeErrorCode errorCode) {
		return switch (errorCode) {
			case QUOTA_EXCEEDED -> QUOTA_EXCEEDED;
			case CHARACTER_REFERENCE_UNAVAILABLE -> CHARACTER_REFERENCE_UNAVAILABLE;
			case IMAGE_GENERATION_FAILED -> IMAGE_GENERATION_FAILED;
			case IMAGE_STORAGE_FAILED -> IMAGE_STORAGE_FAILED;
			case GENERATION_NOT_FOUND -> GENERATION_NOT_FOUND;
		};
	}
}
