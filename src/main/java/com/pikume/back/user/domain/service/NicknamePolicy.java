package com.pikume.back.user.domain.service;

/**
 * 닉네임 점유/검증 도메인 규칙을 담당하는 Domain Service.
 * 닉네임 점유 만료 시간, 사용자 확인 등의 비즈니스 규칙을 캡슐화합니다.
 */
public class NicknamePolicy {

	private static final long HOLD_DURATION_MS = 180_000; // 3분 점유

	/**
	 * 닉네임 점유가 만료되었는지 확인합니다.
	 *
	 * @param holdTimestamp 점유 시작 시간 (millis)
	 * @param currentTimeMs 현재 시간 (millis)
	 * @return 만료 여부
	 */
	public boolean isHoldExpired(long holdTimestamp, long currentTimeMs) {
		return currentTimeMs - holdTimestamp > HOLD_DURATION_MS;
	}

	/**
	 * 점유 지속 시간(밀리초)을 반환합니다.
	 *
	 * @return 점유 지속 시간
	 */
	public long getHoldDurationMs() {
		return HOLD_DURATION_MS;
	}
}
