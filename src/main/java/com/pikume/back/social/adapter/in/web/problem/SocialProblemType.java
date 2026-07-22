package com.pikume.back.social.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import com.pikume.back.social.application.exception.SocialErrorCode;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum SocialProblemType implements ApiProblemType {
	DIARY_NOT_FOUND("https://api.pikume.com/problems/social/diary-not-found", HttpStatus.NOT_FOUND, "Not Found"),
	COMMENT_NOT_FOUND("https://api.pikume.com/problems/social/comment-not-found", HttpStatus.NOT_FOUND, "Not Found"),
	LIKE_NOT_FOUND("https://api.pikume.com/problems/social/like-not-found", HttpStatus.NOT_FOUND, "Not Found"),
	DELETED_COMMENT("https://api.pikume.com/problems/social/deleted-comment", HttpStatus.BAD_REQUEST, "Bad Request"),
	INVALID_PARENT_COMMENT("https://api.pikume.com/problems/social/invalid-parent-comment", HttpStatus.BAD_REQUEST, "Bad Request"),
	UNAUTHORIZED_COMMENT_ACCESS("https://api.pikume.com/problems/social/comment-unauthorized", HttpStatus.UNAUTHORIZED, "Unauthorized"),
	INVALID_COMMENT_REQUEST("https://api.pikume.com/problems/social/invalid-comment-request", HttpStatus.BAD_REQUEST, "Bad Request"),
	PARENT_COMMENT_NOT_IN_SAME_DIARY("https://api.pikume.com/problems/social/parent-comment-not-in-same-diary", HttpStatus.BAD_REQUEST, "Bad Request"),
	ALREADY_LIKED("https://api.pikume.com/problems/social/already-liked", HttpStatus.CONFLICT, "Conflict"),
	DUPLICATE_LIKE("https://api.pikume.com/problems/social/duplicate-like", HttpStatus.CONFLICT, "Conflict"),
	INVALID_FRIEND_REQUEST("https://api.pikume.com/problems/social/invalid-friend-request", HttpStatus.BAD_REQUEST, "Bad Request"),
	ALREADY_FRIENDS("https://api.pikume.com/problems/social/already-friends", HttpStatus.CONFLICT, "Conflict"),
	FRIEND_REQUEST_NOT_FOUND("https://api.pikume.com/problems/social/friend-request-not-found", HttpStatus.NOT_FOUND, "Not Found"),
	FRIEND_NOT_FOUND("https://api.pikume.com/problems/social/friend-not-found", HttpStatus.NOT_FOUND, "Not Found");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	SocialProblemType(String type, HttpStatus status, String title) {
		this.type = URI.create(type);
		this.status = status;
		this.title = title;
	}

	public static SocialProblemType from(SocialErrorCode errorCode) {
		return switch (errorCode) {
			case INVALID_FRIEND_PARTICIPANT, SELF_FRIEND_REQUEST -> INVALID_FRIEND_REQUEST;
			case INVALID_COMMENT_PARTICIPANT -> INVALID_COMMENT_REQUEST;
			case ALREADY_FRIENDS -> ALREADY_FRIENDS;
			case FRIEND_REQUEST_NOT_FOUND, SENT_FRIEND_REQUEST_NOT_FOUND -> FRIEND_REQUEST_NOT_FOUND;
			case FRIEND_NOT_FOUND -> FRIEND_NOT_FOUND;
			case DIARY_NOT_FOUND -> DIARY_NOT_FOUND;
			case COMMENT_NOT_FOUND -> COMMENT_NOT_FOUND;
			case DELETED_COMMENT -> DELETED_COMMENT;
			case INVALID_PARENT_COMMENT -> INVALID_PARENT_COMMENT;
			case PARENT_COMMENT_NOT_IN_SAME_DIARY -> PARENT_COMMENT_NOT_IN_SAME_DIARY;
			case UNAUTHORIZED_COMMENT_ACCESS -> UNAUTHORIZED_COMMENT_ACCESS;
			case ALREADY_LIKED -> ALREADY_LIKED;
			case LIKE_NOT_FOUND -> LIKE_NOT_FOUND;
			case DUPLICATE_LIKE -> DUPLICATE_LIKE;
		};
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
