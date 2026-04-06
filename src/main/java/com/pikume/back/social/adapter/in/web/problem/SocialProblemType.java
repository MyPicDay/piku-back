package com.pikume.back.social.adapter.in.web.problem;

import com.pikume.back.global.error.ApiProblemType;
import com.pikume.back.social.domain.comment.exception.CommentErrorCode;
import com.pikume.back.social.domain.like.exception.LikeErrorCode;
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
	CANNOT_LIKE_OWN_DIARY("https://api.pikume.com/problems/social/cannot-like-own-diary", HttpStatus.BAD_REQUEST, "Bad Request"),
	DUPLICATE_LIKE("https://api.pikume.com/problems/social/duplicate-like", HttpStatus.CONFLICT, "Conflict"),
	INVALID_FRIEND_REQUEST("https://api.pikume.com/problems/social/invalid-friend-request", HttpStatus.BAD_REQUEST,
			"Bad Request"),
	ALREADY_FRIENDS("https://api.pikume.com/problems/social/already-friends", HttpStatus.CONFLICT, "Conflict"),
	FRIEND_REQUEST_NOT_FOUND("https://api.pikume.com/problems/social/friend-request-not-found", HttpStatus.NOT_FOUND,
			"Not Found"),
	FRIEND_NOT_FOUND("https://api.pikume.com/problems/social/friend-not-found", HttpStatus.NOT_FOUND, "Not Found"),
	DATABASE_ERROR("https://api.pikume.com/problems/social/database-error", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
	INTERNAL_SERVER_ERROR("https://api.pikume.com/problems/social/internal-server-error", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

	private final URI type;
	private final HttpStatus status;
	private final String title;

	SocialProblemType(String type, HttpStatus status, String title) {
		this.type = URI.create(type);
		this.status = status;
		this.title = title;
	}

	public static SocialProblemType from(CommentErrorCode errorCode) {
		return switch (errorCode) {
			case DIARY_NOT_FOUND -> DIARY_NOT_FOUND;
			case COMMENT_NOT_FOUND -> COMMENT_NOT_FOUND;
			case DELETED_COMMENT -> DELETED_COMMENT;
			case INVALID_PARENT_COMMENT -> INVALID_PARENT_COMMENT;
			case UNAUTHORIZED_ACCESS -> UNAUTHORIZED_COMMENT_ACCESS;
			case INVALID_REQUEST -> INVALID_COMMENT_REQUEST;
			case PARENT_COMMENT_NOT_IN_SAME_DIARY -> PARENT_COMMENT_NOT_IN_SAME_DIARY;
			case DATABASE_ERROR -> DATABASE_ERROR;
			case INTERNAL_SERVER_ERROR -> INTERNAL_SERVER_ERROR;
		};
	}

	public static SocialProblemType from(LikeErrorCode errorCode) {
		return switch (errorCode) {
			case DIARY_NOT_FOUND -> DIARY_NOT_FOUND;
			case ALREADY_LIKED -> ALREADY_LIKED;
			case LIKE_NOT_FOUND -> LIKE_NOT_FOUND;
			case CANNOT_LIKE_OWN_DIARY -> CANNOT_LIKE_OWN_DIARY;
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
