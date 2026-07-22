package com.pikume.back.social.application.event;

/**
 * Social이 Notification 연동을 위해 공개하는 Application 이벤트입니다.
 */
public sealed interface SocialNotificationEvent {

	record CommentCreated(String receiverId, String senderId, Long diaryId, boolean isReply)
			implements SocialNotificationEvent {
	}

	record LikeCreated(String receiverId, String senderId, Long diaryId) implements SocialNotificationEvent {
	}

	record FriendRequest(String receiverId, String senderId) implements SocialNotificationEvent {
	}

	record FriendAccepted(String receiverId, String senderId) implements SocialNotificationEvent {
	}
}
