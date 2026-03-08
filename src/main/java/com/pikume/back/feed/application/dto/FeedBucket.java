package com.pikume.back.feed.application.dto;

import java.util.List;

public enum FeedBucket {
	NOT_CONSUMED_FRIEND,
	NOT_CONSUMED_PUBLIC,
	CONSUMED_FRIEND,
	CONSUMED_PUBLIC;

	private static final List<FeedBucket> AUTHENTICATED_ORDER = List.of(
			NOT_CONSUMED_FRIEND,
			NOT_CONSUMED_PUBLIC,
			CONSUMED_FRIEND,
			CONSUMED_PUBLIC);

	private static final List<FeedBucket> ANONYMOUS_ORDER = List.of(NOT_CONSUMED_PUBLIC);

	public static List<FeedBucket> orderedBuckets(String userId) {
		return hasUser(userId) ? AUTHENTICATED_ORDER : ANONYMOUS_ORDER;
	}

	public static FeedBucket firstBucket(String userId) {
		return orderedBuckets(userId).get(0);
	}

	public FeedBucket next(String userId) {
		List<FeedBucket> orderedBuckets = orderedBuckets(userId);
		int index = orderedBuckets.indexOf(this);
		if (index < 0 || index + 1 >= orderedBuckets.size()) {
			return null;
		}
		return orderedBuckets.get(index + 1);
	}

	public boolean isFriendBucket() {
		return this == NOT_CONSUMED_FRIEND || this == CONSUMED_FRIEND;
	}

	public boolean isConsumedBucket() {
		return this == CONSUMED_FRIEND || this == CONSUMED_PUBLIC;
	}

	public static boolean hasUser(String userId) {
		return userId != null && !userId.isBlank();
	}
}
