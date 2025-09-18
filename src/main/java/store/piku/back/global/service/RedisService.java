package store.piku.back.global.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 지정된 작업에 대한 일일 요청 횟수를 확인하고 제한합니다. (날짜 기준 초기화)
     * @param actionPrefix 작업 종류 (예: "ai_generate")
     * @param userId 사용자 ID
     * @param limit 하루 최대 허용 횟수
     * @return 횟수 초과 시 true, 아닐 시 false
     */
    public boolean isRequestLimitExceeded(String actionPrefix, String userId, int limit) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String key = actionPrefix + ":" + userId + ":" + today;

        Long currentRequests = redisTemplate.opsForValue().increment(key);

        // 첫 요청이라면, 만료 시간을 오늘 자정까지로 설정
        if (currentRequests == 1) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            Duration durationUntilEndOfDay = Duration.between(now, endOfDay);

            redisTemplate.expire(key, durationUntilEndOfDay);
        }

        return currentRequests > limit;
    }


    /**
     * 현재 요청 횟수가 제한을 초과했는지 확인합니다.
     * @param actionPrefix 작업 종류 (예: "ai_generate")
     * @param userId 사용자 ID
     * @param limit 하루 최대 허용 횟수
     * @return 횟수 초과 시 true, 아닐 시 false
     */
    public boolean isLimitExceeded(String actionPrefix, String userId, int limit) {
        String key = buildKey(actionPrefix, userId);
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return false;
        }

        int currentRequests = Integer.parseInt(value);
        return currentRequests >= limit;
    }

    /**
     * 요청 횟수를 1 증가시키고, 만료 시간을 설정합니다.
     */
    public void incrementRequestCount(String actionPrefix, String userId) {
        String key = buildKey(actionPrefix, userId);

        Long currentRequests = redisTemplate.opsForValue().increment(key);

        if (currentRequests != null && currentRequests == 1) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            Duration durationUntilEndOfDay = Duration.between(now, endOfDay);

            redisTemplate.expire(key, durationUntilEndOfDay);
        }
    }

    /**
     * Redis 키를 생성하는 헬퍼 메소드
     */
    private String buildKey(String actionPrefix, String userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return actionPrefix + ":" + userId + ":" + today;
    }

    /**
     * 사용자의 일일 요청 가능 횟수 중 '남은 횟수'를 반환합니다.
     * @param actionPrefix 행동 종류
     * @param userId 사용자 ID
     * @param limit 일일 전체 제한 횟수
     * @return 남은 요청 횟수
     */
    public int getRemainingCount(String actionPrefix, String userId, int limit) {
        String key = buildKey(actionPrefix, userId);
        String value = redisTemplate.opsForValue().get(key);

        // 1. Redis에 키가 없는 경우 (오늘 한 번도 사용 안 함)
        // -> 남은 횟수는 전체 횟수와 동일합니다.
        if (value == null) {
            return limit;
        }

        // 2. Redis에 키가 있는 경우
        // -> (전체 제한 횟수 - 현재 사용 횟수)를 계산합니다.
        int currentRequests = Integer.parseInt(value);
        int remaining = limit - currentRequests;

        // 3. 만약 계산 결과가 음수이면 0을 반환합니다.
        return Math.max(0, remaining);
    }
}