package com.pikume.back.social.domain.event;

/**
 * Social 도메인에서 발행하는 이벤트 클래스들.
 * NotificationService 직접 참조를 제거하고 이벤트 기반으로 전환.
 */
public final class SocialEvent {

	private SocialEvent() {
	}

	public record CommentCreatedEvent(String receiverId, String senderId, Long diaryId, boolean isReply) {
	}

	public record LikeCreatedEvent(String receiverId, String senderId, Long diaryId) {
	}

	public record FriendRequestEvent(String receiverId, String senderId) {
	}

	public record FriendAcceptedEvent(String receiverId, String senderId) {
	}
}
