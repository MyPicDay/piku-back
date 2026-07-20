package com.pikume.back.notification.application.policy;

import com.pikume.back.notification.application.dto.NotificationKind;
import org.springframework.stereotype.Component;

@Component
public class NotificationPresentationPolicy {

	private static final String ANONYMOUS_NICKNAME = "익명";

	public String messageFor(NotificationKind kind) {
		return switch (kind) {
			case FRIEND_REQUEST -> "님이 친구 요청을 보냈습니다";
			case FRIEND_ACCEPT -> "님이 친구 수락했습니다.";
			case COMMENT -> "님이 일기에 댓글을 달았습니다.";
			case REPLY -> "님이 회원님의 댓글에 답글을 달았습니다.";
			case FRIEND_DIARY -> "님이 새 일기를 작성하였습니다.";
			case LIKE -> "님이 회원님의 일기를 좋아합니다.";
		};
	}

	public String senderNickname(boolean anonymous, String nickname) {
		return anonymous ? ANONYMOUS_NICKNAME : nickname;
	}
}
