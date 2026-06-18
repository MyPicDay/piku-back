package com.pikume.back.global.config;

import com.pikume.back.admin.application.port.out.AdminSessionCacheEntry;
import com.pikume.back.admin.domain.AdminSessionPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("RedisConfig")
class RedisConfigTest {

	@Test
	@DisplayName("관리자 세션 캐시 엔트리를 실제 Redis 값 직렬화기로 왕복 변환한다")
	void roundTripsAdminSessionCacheEntry() {
		RedisTemplate<String, Object> template =
				new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
		@SuppressWarnings("unchecked")
		RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();
		AdminSessionCacheEntry entry = new AdminSessionCacheEntry(
				"session-1",
				"admin-1",
				AdminSessionPhase.AUTHENTICATED,
				"session-hash",
				"csrf-hash",
				3L,
				LocalDateTime.of(2026, 6, 18, 18, 0),
				LocalDateTime.of(2026, 6, 18, 10, 30));

		byte[] serialized = serializer.serialize(entry);
		Object deserialized = serializer.deserialize(serialized);

		assertThat(deserialized).isEqualTo(entry);
	}
}
