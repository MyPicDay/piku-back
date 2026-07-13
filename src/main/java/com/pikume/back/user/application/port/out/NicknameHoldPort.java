package com.pikume.back.user.application.port.out;

import java.time.Instant;

public interface NicknameHoldPort {

	boolean tryAcquire(String nickname, String userId, Instant requestedAt);

	boolean isHeldBy(String nickname, String userId, Instant checkedAt);

	void release(String nickname, String userId);
}
