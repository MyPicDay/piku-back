package com.pikume.back.feed.adapter.in.web;

import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.dto.FeedSortMode;
import com.pikume.back.feed.application.exception.InvalidFeedSortException;

@Component
public class FeedSortRequestMapper {

	public FeedSortMode map(String value) {
		if (value == null || value.isBlank()) {
			return FeedSortMode.RECOMMENDED;
		}

		String normalizedValue = value.trim();
		if ("recommended".equalsIgnoreCase(normalizedValue)) {
			return FeedSortMode.RECOMMENDED;
		}
		if ("latest".equalsIgnoreCase(normalizedValue)) {
			return FeedSortMode.LATEST;
		}
		throw new InvalidFeedSortException();
	}
}
