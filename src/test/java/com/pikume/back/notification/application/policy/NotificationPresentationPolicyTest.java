package com.pikume.back.notification.application.policy;

import com.pikume.back.notification.application.dto.NotificationKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationPresentationPolicy")
class NotificationPresentationPolicyTest {

	private final NotificationPresentationPolicy policy = new NotificationPresentationPolicy();

	@Nested
	@DisplayName("messageFor - 알림 문구")
	class MessageFor {

		@Test
		@DisplayName("알림 종류별 기존 문구를 반환한다")
		void returnsExistingMessagesForEveryKind() {
			assertThat(policy.messageFor(NotificationKind.FRIEND_REQUEST)).isEqualTo("님이 친구 요청을 보냈습니다");
			assertThat(policy.messageFor(NotificationKind.FRIEND_ACCEPT)).isEqualTo("님이 친구 수락했습니다.");
			assertThat(policy.messageFor(NotificationKind.COMMENT)).isEqualTo("님이 일기에 댓글을 달았습니다.");
			assertThat(policy.messageFor(NotificationKind.REPLY)).isEqualTo("님이 회원님의 댓글에 답글을 달았습니다.");
			assertThat(policy.messageFor(NotificationKind.FRIEND_DIARY)).isEqualTo("님이 새 일기를 작성하였습니다.");
			assertThat(policy.messageFor(NotificationKind.LIKE)).isEqualTo("님이 회원님의 일기를 좋아합니다.");
		}
	}

	@Nested
	@DisplayName("senderNickname - 발신자 표시")
	class SenderNickname {

		@Test
		@DisplayName("익명 알림은 발신자 닉네임을 익명으로 표시한다")
		void masksAnonymousSender() {
			assertThat(policy.senderNickname(true, "실제 닉네임")).isEqualTo("익명");
		}

		@Test
		@DisplayName("비익명 알림은 실제 발신자 닉네임을 유지한다")
		void preservesNamedSender() {
			assertThat(policy.senderNickname(false, "실제 닉네임")).isEqualTo("실제 닉네임");
		}
	}
}
