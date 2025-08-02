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
}