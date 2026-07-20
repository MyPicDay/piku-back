package com.pikume.back.notification.adapter.in.event;

import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.dto.RecordNotificationCommand;
import com.pikume.back.notification.application.port.in.RecordNotificationUseCase;
import com.pikume.back.social.application.event.SocialNotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialEventListener")
class SocialEventListenerTest {

	@InjectMocks
	private SocialEventListener listener;

	@Mock
	private RecordNotificationUseCase recordNotificationUseCase;

	@Test
	@DisplayName("Social 댓글 이벤트를 Notification 소유 기록 명령으로 번역한다")
	void translatesCommentEvent() {
		listener.handleCommentCreated(new SocialNotificationEvent.CommentCreated(
				"receiver-id",
				"sender-id",
				10L,
				false));

		ArgumentCaptor<RecordNotificationCommand> commandCaptor =
				ArgumentCaptor.forClass(RecordNotificationCommand.class);
		then(recordNotificationUseCase).should().recordNotification(commandCaptor.capture());
		assertThat(commandCaptor.getValue()).isEqualTo(new RecordNotificationCommand(
				"receiver-id",
				NotificationKind.COMMENT,
				"sender-id",
				10L));
	}

	@Test
	@DisplayName("Social 답글 이벤트를 REPLY 알림 기록 명령으로 번역한다")
	void translatesReplyEvent() {
		listener.handleCommentCreated(new SocialNotificationEvent.CommentCreated(
				"receiver-id",
				"sender-id",
				10L,
				true));

		ArgumentCaptor<RecordNotificationCommand> commandCaptor =
				ArgumentCaptor.forClass(RecordNotificationCommand.class);
		then(recordNotificationUseCase).should().recordNotification(commandCaptor.capture());
		assertThat(commandCaptor.getValue().kind()).isEqualTo(NotificationKind.REPLY);
	}
}
