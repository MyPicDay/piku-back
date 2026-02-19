package store.piku.back.notification.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.notification.application.port.out.PushNotificationPort;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenService - FCM 토큰 관리")
class FcmTokenServiceTest {

	@InjectMocks
	private FcmTokenService fcmTokenService;

	@Mock
	private PushNotificationPort pushNotificationPort;

	@Nested
	@DisplayName("saveToken - FCM 토큰 저장")
	class SaveToken {

		@Test
		@DisplayName("FCM 토큰 저장을 PushNotificationPort에 위임한다")
		void delegatesToPushPort() {
			fcmTokenService.saveToken("user-id", "token-123", "device-1");

			then(pushNotificationPort).should().saveToken("user-id", "token-123", "device-1");
		}

		@Test
		@DisplayName("서로 다른 디바이스의 토큰을 각각 저장한다")
		void savesTokenForDifferentDevices() {
			fcmTokenService.saveToken("user-id", "token-1", "device-a");
			fcmTokenService.saveToken("user-id", "token-2", "device-b");

			then(pushNotificationPort).should().saveToken("user-id", "token-1", "device-a");
			then(pushNotificationPort).should().saveToken("user-id", "token-2", "device-b");
		}
	}
}
