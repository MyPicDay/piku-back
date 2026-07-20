package com.pikume.back.notification.adapter.out.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("LocalPushTokenAdapter")
class LocalPushTokenAdapterTest {

	private final LocalPushTokenAdapter adapter = new LocalPushTokenAdapter();

	@Test
	@DisplayName("비운영 환경에서는 Push Token 등록·조회·해제를 무저장으로 처리한다")
	void keepsTokensUnstoredOutsideProduction() {
		assertThatCode(() -> {
			adapter.registerPushToken("user-id", "token-1", "device-1");
			adapter.revokePushToken("token-1");
			adapter.revokePushTokenForDevice("user-id", "device-1");
		}).doesNotThrowAnyException();

		assertThat(adapter.loadPushDeliveryTokens("user-id")).isEmpty();
	}
}
