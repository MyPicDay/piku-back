package com.pikume.back.user.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface NicknameHoldPort {

	/** Serialize nickname availability, reservations, and account writes until transaction completion.
	 * Always acquire before locking a user or reading nickname availability. */
	void lockNicknameWrites();

	boolean tryAcquire(String nickname, String userId, Instant requestedAt);

	boolean isHeldBy(String nickname, String userId, Instant checkedAt);

	boolean isHeld(String nickname, Instant checkedAt);

	Optional<Instant> heldUntil(String nickname, String userId, Instant checkedAt);

	void release(String nickname, String userId);

	void releaseForUser(String userId);
}
