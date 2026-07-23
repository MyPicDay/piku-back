package com.pikume.back.support.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import com.pikume.back.support.application.exception.SupportErrorCode;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum SupportProblemType implements ApiProblemType {
	SUBMITTER_NOT_FOUND(
			"https://api.pikume.com/problems/support/submitter-not-found",
			HttpStatus.NOT_FOUND,
			"Not Found"),
	INVALID_INQUIRY(
			"https://api.pikume.com/problems/support/invalid-inquiry",
			HttpStatus.BAD_REQUEST,
			"Bad Request"),
	ATTACHMENT_STORAGE_FAILED(
			"https://api.pikume.com/problems/support/attachment-storage-failed",
			HttpStatus.INTERNAL_SERVER_ERROR,
			"Internal Server Error");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	SupportProblemType(String type, HttpStatus status, String title) {
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

	public static SupportProblemType from(SupportErrorCode errorCode) {
		return switch (errorCode) {
			case SUBMITTER_NOT_FOUND -> SUBMITTER_NOT_FOUND;
			case INVALID_INQUIRY -> INVALID_INQUIRY;
			case ATTACHMENT_STORAGE_FAILED -> ATTACHMENT_STORAGE_FAILED;
		};
	}
}
