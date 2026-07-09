package com.pikume.back.security.adapter.out.crosscontext;

import com.pikume.back.notification.application.port.in.FcmTokenUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationAdapterForSecurity")
class NotificationAdapterForSecurityTest {

	@InjectMocks
	private NotificationAdapterForSecurity notificationAdapterForSecurity;

	@Mock
	private FcmTokenUseCase fcmTokenUseCase;

	@Test
	@DisplayName("로그아웃된 기기의 푸시 토큰 해제를 notification 유스케이스에 위임한다")
	void revokeDevicePushTokenDelegatesToNotificationUseCase() {
		notificationAdapterForSecurity.revokeDevicePushToken("user-id", "device-1");

		then(fcmTokenUseCase).should().revokeTokenForDevice("user-id", "device-1");
	}
}
