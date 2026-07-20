package com.pikume.back.user.adapter.out.crosscontext;

import com.pikume.back.notification.application.port.in.RevokePushTokenUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("NotificationAdapterForUser")
class NotificationAdapterForUserTest {

	private final RevokePushTokenUseCase revokePushTokenUseCase =
			mock(RevokePushTokenUseCase.class);
	private final NotificationAdapterForUser adapter =
			new NotificationAdapterForUser(revokePushTokenUseCase);

	@Test
	@DisplayName("기기 로그아웃을 Push Token 해제 Use Case로 번역한다")
	void revokesDevicePushToken() {
		adapter.revokeDevicePushToken("user-id", "device-1");

		then(revokePushTokenUseCase).should()
				.revokePushTokenForDevice("user-id", "device-1");
	}
}
