package com.pikume.back.social.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Social 도메인에서 발행하는 이벤트 클래스들.
 * NotificationService 직접 참조를 제거하고 이벤트 기반으로 전환.
 */
public class SocialEvent {

	@Getter
	@RequiredArgsConstructor
	public static class CommentCreatedEvent {
		private final String receiverId;
		private final String senderId;
		private final Long diaryId;
		private final boolean isReply;
	}

	@Getter
	@RequiredArgsConstructor
	public static class LikeCreatedEvent {
		private final String receiverId;
		private final String senderId;
		private final Long diaryId;
	}

	@Getter
	@RequiredArgsConstructor
	public static class FriendRequestEvent {
		private final String receiverId;
		private final String senderId;
	}

	@Getter
	@RequiredArgsConstructor
	public static class FriendAcceptedEvent {
		private final String receiverId;
		private final String senderId;
	}
}
