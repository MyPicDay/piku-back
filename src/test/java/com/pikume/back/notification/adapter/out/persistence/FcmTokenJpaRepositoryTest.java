package com.pikume.back.notification.adapter.out.persistence;

import com.pikume.back.notification.domain.FcmToken;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FcmTokenJpaRepository")
class FcmTokenJpaRepositoryTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private FcmTokenJpaRepository fcmTokenJpaRepository;

	@Test
	@DisplayName("userId와 deviceId 조합에 해당하는 FCM 토큰만 삭제한다")
	void deleteByUserIdAndDeviceIdDeletesOnlyMatchingUserDevicePair() {
		fcmTokenJpaRepository.save(new FcmToken("user-a", "token-a", "shared-device"));
		fcmTokenJpaRepository.save(new FcmToken("user-b", "token-b", "shared-device"));
		fcmTokenJpaRepository.save(new FcmToken("user-a", "token-c", "other-device"));

		fcmTokenJpaRepository.deleteByUserIdAndDeviceId("user-a", "shared-device");
		flushAndClear();

		assertThat(fcmTokenJpaRepository.findAll())
				.extracting(FcmToken::getToken)
				.containsExactlyInAnyOrder("token-b", "token-c");
	}
}
