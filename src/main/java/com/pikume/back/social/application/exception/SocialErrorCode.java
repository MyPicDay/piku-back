package com.pikume.back.social.application.exception;

public enum SocialErrorCode {
	INVALID_FRIEND_PARTICIPANT("사용자를 찾을 수 없습니다."),
	INVALID_COMMENT_PARTICIPANT("다이어리 정보가 유효하지 않습니다."),
	SELF_FRIEND_REQUEST("자신에게 요청 할 수 없습니다."),
	ALREADY_FRIENDS("이미 친구입니다."),
	FRIEND_REQUEST_NOT_FOUND("해당 친구 요청 기록을 찾을 수 없습니다."),
	SENT_FRIEND_REQUEST_NOT_FOUND("요청 보낸 기록이 없습니다."),
	FRIEND_NOT_FOUND("친구 관계가 존재하지 않습니다."),
	DIARY_NOT_FOUND("해당 일기를 찾을 수 없습니다."),
	COMMENT_NOT_FOUND("해당 댓글을 찾을 수 없습니다."),
	DELETED_COMMENT("이미 삭제된 댓글입니다."),
	INVALID_PARENT_COMMENT("대댓글에 대댓글을 달 수 없습니다."),
	PARENT_COMMENT_NOT_IN_SAME_DIARY("댓글이 다이어리에 소속되어 있지 않습니다."),
	UNAUTHORIZED_COMMENT_ACCESS("권한이 없는 사용자입니다."),
	ALREADY_LIKED("이미 좋아요한 일기입니다."),
	LIKE_NOT_FOUND("좋아요 기록을 찾을 수 없습니다."),
	DUPLICATE_LIKE("좋아요 중복 저장이 감지되었습니다.");

	private final String message;

	SocialErrorCode(String message) {
		this.message = message;
	}

	public String message() {
		return message;
	}
}
