package com.pikume.back.notification.adapter.out.crosscontext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.user.application.dto.UserSummaryView;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.port.in.QueryUserSummaryUseCase;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("UserAdapterForNotification")
class UserAdapterForNotificationTest {

	private final QueryUserSummaryUseCase queryUserSummaryUseCase = mock(QueryUserSummaryUseCase.class);
	private final ResolveObjectUrlPort resolveObjectUrlPort = mock(ResolveObjectUrlPort.class);
	private final UserAdapterForNotification adapter = new UserAdapterForNotification(
			queryUserSummaryUseCase, resolveObjectUrlPort);

	@Test
	@DisplayName("User 공개 계약을 Notification 소유 발신자 View로 일괄 번역한다")
	void loadsNotificationSenders() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1", "missing")))
				.willReturn(Map.of(
						"user-1",
						new UserSummaryView(
								"user-1", "피쿠",
								new UserAvatarReference("avatars/user-1.png", false, false))));
		given(resolveObjectUrlPort.resolveObjectUrl("avatars/user-1.png", false))
				.willReturn("avatar-url");

		var result = adapter.loadNotificationSenders(Set.of("user-1", "missing"));

		assertThat(result).containsOnlyKeys("user-1");
		assertThat(result.get("user-1").nickname()).isEqualTo("피쿠");
		assertThat(result.get("user-1").avatarUrl()).isEqualTo("avatar-url");
	}

	@Test
	@DisplayName("아바타 메타데이터가 없는 발신자는 null 아바타 URL로 번역한다")
	void preservesMissingAvatarMetadata() {
		given(queryUserSummaryUseCase.queryUserSummaries(Set.of("user-1")))
				.willReturn(Map.of(
						"user-1",
						new UserSummaryView("user-1", "피쿠", null)));

		var result = adapter.loadNotificationSenders(Set.of("user-1"));

		assertThat(result.get("user-1").avatarUrl()).isNull();
		then(resolveObjectUrlPort).shouldHaveNoInteractions();
	}
}
