package com.pikume.back.admin.adapter.out.cache;

import com.pikume.back.admin.application.port.out.AdminSessionCacheEntry;
import com.pikume.back.admin.domain.AdminSessionPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisAdminSessionCacheAdapter")
class RedisAdminSessionCacheAdapterTest {

	@Mock
	private RedisTemplate<String, Object> redisTemplate;
	@Mock
	private ValueOperations<String, Object> valueOperations;

	@Test
	@DisplayName("관리자 세션 전용 네임스페이스에서 캐시를 읽는다")
	void readsFromDedicatedNamespace() {
		AdminSessionCacheEntry entry = entry(LocalDateTime.now().plusMinutes(10));
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get("admin-session:session-hash")).willReturn(entry);

		Optional<AdminSessionCacheEntry> result = adapter().findByTokenHash("session-hash");

		assertThat(result).contains(entry);
	}

	@Test
	@DisplayName("세션의 남은 유효시간 안에서 캐시 TTL을 설정한다")
	void storesWithRemainingSessionTtl() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		AdminSessionCacheEntry entry = entry(LocalDateTime.now().plusMinutes(10));

		adapter().put(entry);

		then(valueOperations).should().set(
				org.mockito.ArgumentMatchers.eq("admin-session:session-hash"),
				org.mockito.ArgumentMatchers.eq(entry),
				any(Duration.class));
	}

	@Test
	@DisplayName("이미 만료된 세션은 캐시에 다시 저장하지 않는다")
	void doesNotStoreExpiredSession() {
		adapter().put(entry(LocalDateTime.now().minusSeconds(1)));

		then(valueOperations).should(never()).set(any(), any(), any(Duration.class));
	}

	private RedisAdminSessionCacheAdapter adapter() {
		return new RedisAdminSessionCacheAdapter(redisTemplate);
	}

	private AdminSessionCacheEntry entry(LocalDateTime idleExpiresAt) {
		return new AdminSessionCacheEntry(
				"session-1",
				"admin-1",
				AdminSessionPhase.AUTHENTICATED,
				"session-hash",
				"csrf-hash",
				2L,
				LocalDateTime.now().plusHours(8),
				idleExpiresAt);
	}
}
