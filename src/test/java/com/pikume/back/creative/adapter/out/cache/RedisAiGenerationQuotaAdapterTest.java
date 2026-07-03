package com.pikume.back.creative.adapter.out.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisAiGenerationQuotaAdapter")
class RedisAiGenerationQuotaAdapterTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
			Instant.parse("2026-07-03T01:00:00Z"),
			ZoneId.of("Asia/Seoul"));

	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	@DisplayName("기존 Redis 키 형식으로 사용량을 읽는다")
	void readsUsageCountWithCompatibleKey() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get("ai_generate:user1:2026-07-03")).willReturn("2");

		int result = adapter().getUsageCount("ai_generate", "user1");

		assertThat(result).isEqualTo(2);
	}

	@Test
	@DisplayName("Redis 값이 없으면 사용량은 0이다")
	void returnsZeroWhenNoUsageExists() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get("ai_generate:user1:2026-07-03")).willReturn(null);

		int result = adapter().getUsageCount("ai_generate", "user1");

		assertThat(result).isZero();
	}

	@Test
	@DisplayName("한도 미만이면 원자적으로 사용량을 선차감한다")
	void consumesWhenUsageIsBelowLimit() {
		given(redisTemplate.execute(any(),
				eq(List.of("ai_generate:user1:2026-07-03")),
				eq("3"),
				anyString()))
				.willReturn(2L);

		Optional<Integer> result = adapter().consumeIfAvailable("ai_generate", "user1", 3);

		assertThat(result).contains(2);
	}

	@Test
	@DisplayName("한도에 도달하면 사용량을 선차감하지 않는다")
	void rejectsConsumptionWhenLimitIsReached() {
		given(redisTemplate.execute(any(),
				eq(List.of("ai_generate:user1:2026-07-03")),
				eq("3"),
				anyString()))
				.willReturn(-1L);

		Optional<Integer> result = adapter().consumeIfAvailable("ai_generate", "user1", 3);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("선차감 시 당일 종료 시각까지의 TTL을 Redis 원자 연산에 전달한다")
	void passesTtlUntilEndOfDayToConsumptionScript() {
		given(redisTemplate.execute(any(),
				eq(List.of("ai_generate:user1:2026-07-03")),
				eq("3"),
				anyString()))
				.willReturn(1L);
		ArgumentCaptor<String> ttlCaptor = ArgumentCaptor.forClass(String.class);

		adapter().consumeIfAvailable("ai_generate", "user1", 3);

		then(redisTemplate).should().execute(any(),
				eq(List.of("ai_generate:user1:2026-07-03")),
				eq("3"),
				ttlCaptor.capture());
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(FIXED_CLOCK), LocalTime.MAX);
		Duration expectedTtl = Duration.between(now, endOfDay);
		assertThat(Long.parseLong(ttlCaptor.getValue())).isEqualTo(expectedTtl.toMillis());
	}

	@Test
	@DisplayName("차감 취소는 Redis에서 원자적으로 수행한다")
	void releasesConsumptionAtomically() {
		given(redisTemplate.execute(any(),
				eq(List.of("ai_generate:user1:2026-07-03"))))
				.willReturn(1L);

		adapter().releaseConsumption("ai_generate", "user1");

		then(redisTemplate).should().execute(any(),
				eq(List.of("ai_generate:user1:2026-07-03")));
	}

	private RedisAiGenerationQuotaAdapter adapter() {
		return new RedisAiGenerationQuotaAdapter(redisTemplate, FIXED_CLOCK);
	}
}
