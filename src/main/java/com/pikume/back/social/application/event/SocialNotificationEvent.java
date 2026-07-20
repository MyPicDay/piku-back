package com.pikume.back.social.application.event;

/**
 * Social이 Notification 연동을 위해 공개하는 Application 이벤트입니다.
 */
public final class SocialNotificationEvent {

	private SocialNotificationEvent() {
	}

	public record CommentCreated(String receiverId, String senderId, Long diaryId, boolean isReply) {
	}

	public record LikeCreated(String receiverId, String senderId, Long diaryId) {
	}

	public record FriendRequest(String receiverId, String senderId) {
	}

	public record FriendAccepted(String receiverId, String senderId) {
	}
}
