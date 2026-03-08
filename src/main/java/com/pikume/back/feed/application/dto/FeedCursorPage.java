package com.pikume.back.feed.application.dto;

import java.util.List;

public record FeedCursorPage<T>(
		List<T> items,
		String nextCursor,
		boolean hasNext
) {
}
