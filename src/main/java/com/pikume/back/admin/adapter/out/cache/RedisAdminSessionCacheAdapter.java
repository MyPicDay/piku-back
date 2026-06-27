package com.pikume.back.admin.adapter.out.cache;

import com.pikume.back.admin.application.port.out.AdminSessionCacheEntry;
import com.pikume.back.admin.application.port.out.AdminSessionCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisAdminSessionCacheAdapter implements AdminSessionCachePort {

	private static final String KEY_PREFIX = "admin-session:";

	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	public Optional<AdminSessionCacheEntry> findByTokenHash(String sessionTokenHash) {
		Object cached = redisTemplate.opsForValue().get(key(sessionTokenHash));
		return cached instanceof AdminSessionCacheEntry entry ? Optional.of(entry) : Optional.empty();
	}

	@Override
	public void put(AdminSessionCacheEntry entry) {
		LocalDateTime expiresAt = entry.idleExpiresAt().isBefore(entry.absoluteExpiresAt())
				? entry.idleExpiresAt()
				: entry.absoluteExpiresAt();
		Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
		if (ttl.isZero() || ttl.isNegative()) {
			return;
		}
		redisTemplate.opsForValue().set(key(entry.sessionTokenHash()), entry, ttl);
	}

	@Override
	public void evict(String sessionTokenHash) {
		redisTemplate.delete(key(sessionTokenHash));
	}

	private String key(String sessionTokenHash) {
		return KEY_PREFIX + sessionTokenHash;
	}
}
