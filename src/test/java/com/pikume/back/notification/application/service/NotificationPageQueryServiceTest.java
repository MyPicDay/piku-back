package com.pikume.back.notification.application.service;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.notification.application.dto.NotificationKind;
import com.pikume.back.notification.application.dto.NotificationResult;
import com.pikume.back.notification.application.port.out.LoadNotificationPagePort;
import com.pikume.back.notification.application.readmodel.NotificationListItemView;
import com.pikume.back.notification.domain.Notification;
import com.pikume.back.notification.domain.vo.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPageQueryService")
class NotificationPageQueryServiceTest {

	@InjectMocks
	private NotificationPageQueryService service;

	@Mock
	private LoadNotificationPagePort loadNotificationPagePort;
	@Mock
	private NotificationListAssembler notificationListAssembler;

	@Test
	@DisplayName("Notification 원본 Page를 표시 항목으로 조합해 반환한다")
	void queriesAssembledNotificationPage() {
		PageQuery pageQuery = PageQuery.of(0, 10);
		PageResult<Notification> source = new PageResult<>(
				List.of(new Notification(
						1L,
						"receiver-id",
						"sender-id",
						NotificationType.COMMENT,
						10L,
						false,
						null)),
				0,
				10,
				1);
		NotificationListItemView item = new NotificationListItemView(
				1L,
				"님이 일기에 댓글을 달았습니다.",
				"보낸이",
				"avatar-url",
				NotificationKind.COMMENT,
				10L,
				"thumbnail-url",
				false,
				null,
				null,
				"owner-id");
		given(loadNotificationPagePort.loadNotificationPage("receiver-id", pageQuery)).willReturn(source);
		given(notificationListAssembler.assemble(source))
				.willReturn(new PageResult<>(List.of(item), 0, 10, 1));

		PageResult<NotificationResult> result = service.queryNotifications("receiver-id", pageQuery);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).type()).isEqualTo(NotificationKind.COMMENT);
		assertThat(result.getContent().get(0).nickname()).isEqualTo("보낸이");
		assertThat(result.getTotalElements()).isEqualTo(1);
	}
}
