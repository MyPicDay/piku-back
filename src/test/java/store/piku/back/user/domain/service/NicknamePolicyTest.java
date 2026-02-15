package store.piku.back.user.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NicknamePolicy Domain Service")
class NicknamePolicyTest {

	private final NicknamePolicy nicknamePolicy = new NicknamePolicy();

	@Test
	@DisplayName("점유 만료 시간 이내이면 만료되지 않았다")
	void holdNotExpired() {
		long now = System.currentTimeMillis();
		long holdTimestamp = now - 60_000; // 1분 전

		assertThat(nicknamePolicy.isHoldExpired(holdTimestamp, now)).isFalse();
	}

	@Test
	@DisplayName("점유 만료 시간을 초과하면 만료된다")
	void holdExpired() {
		long now = System.currentTimeMillis();
		long holdTimestamp = now - 200_000; // 3분 20초 전

		assertThat(nicknamePolicy.isHoldExpired(holdTimestamp, now)).isTrue();
	}

	@Test
	@DisplayName("정확히 3분이면 만료되지 않았다")
	void holdExactlyAtBoundary() {
		long now = System.currentTimeMillis();
		long holdTimestamp = now - 180_000; // 정확히 3분

		assertThat(nicknamePolicy.isHoldExpired(holdTimestamp, now)).isFalse();
	}

	@Test
	@DisplayName("점유 지속 시간은 3분(180,000ms)이다")
	void holdDuration() {
		assertThat(nicknamePolicy.getHoldDurationMs()).isEqualTo(180_000L);
	}
}
