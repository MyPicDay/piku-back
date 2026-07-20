package com.pikume.back.notification.adapter.in.web;

import com.pikume.back.notification.adapter.in.web.dto.FcmTokenRequest;
import com.pikume.back.notification.application.port.in.RegisterPushTokenUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("FcmController")
class FcmControllerTest {

	private final RegisterPushTokenUseCase registerPushTokenUseCase =
			mock(RegisterPushTokenUseCase.class);
	private final FcmController controller = new FcmController(registerPushTokenUseCase);

	@Test
	@DisplayName("POST /api/fcm은 현재 요청 사용자 ID와 Token 정보를 등록 Use Case에 전달한다")
	void registersPushToken() {
		var response = controller.saveToken(new FcmTokenRequest(
				"user-id",
				"token-123",
				"device-1"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		then(registerPushTokenUseCase).should()
				.registerPushToken("user-id", "token-123", "device-1");
	}
}
