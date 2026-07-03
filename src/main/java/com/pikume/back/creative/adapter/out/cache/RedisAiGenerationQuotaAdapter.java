package com.pikume.back.creative.adapter.out.cache;

import com.pikume.back.creative.application.port.out.AiGenerationQuotaPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class RedisAiGenerationQuotaAdapter implements AiGenerationQuotaPort {

	private static final RedisScript<Long> CONSUME_IF_AVAILABLE_SCRIPT = RedisScript.of("""
			local current = tonumber(redis.call('get', KEYS[1]) or '0')
			local limit = tonumber(ARGV[1])
			if current >= limit then
				return -1
			end
			local updated = redis.call('incr', KEYS[1])
			if updated == 1 then
				redis.call('pexpire', KEYS[1], ARGV[2])
			end
			return updated
			""", Long.class);
	private static final RedisScript<Long> RELEASE_CONSUMPTION_SCRIPT = RedisScript.of("""
			local current = tonumber(redis.call('get', KEYS[1]) or '0')
			if current <= 0 then
				return 0
			end
			local updated = redis.call('decr', KEYS[1])
			if updated <= 0 then
				redis.call('del', KEYS[1])
				return 0
			end
			return updated
			""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final Clock clock;

	@Autowired
	public RedisAiGenerationQuotaAdapter(StringRedisTemplate redisTemplate) {
		this(redisTemplate, Clock.systemDefaultZone());
	}

	RedisAiGenerationQuotaAdapter(StringRedisTemplate redisTemplate, Clock clock) {
		this.redisTemplate = redisTemplate;
		this.clock = clock;
	}

	@Override
	public int getUsageCount(String quotaName, String userId) {
		String value = redisTemplate.opsForValue().get(buildKey(quotaName, userId));

		if (value == null) {
			return 0;
		}

		return Integer.parseInt(value);
	}

	@Override
	public Optional<Integer> consumeIfAvailable(String quotaName, String userId, int limit) {
		String key = buildKey(quotaName, userId);
		Long result = redisTemplate.execute(
				CONSUME_IF_AVAILABLE_SCRIPT,
				List.of(key),
				String.valueOf(limit),
				String.valueOf(durationUntilEndOfDay().toMillis()));

		if (result == null || result < 0) {
			return Optional.empty();
		}

		return Optional.of(result.intValue());
	}

	@Override
	public void releaseConsumption(String quotaName, String userId) {
		redisTemplate.execute(RELEASE_CONSUMPTION_SCRIPT, List.of(buildKey(quotaName, userId)));
	}

	private String buildKey(String quotaName, String userId) {
		String today = LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE);
		return quotaName + ":" + userId + ":" + today;
	}

	private Duration durationUntilEndOfDay() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(clock), LocalTime.MAX);
		return Duration.between(now, endOfDay);
	}
}
