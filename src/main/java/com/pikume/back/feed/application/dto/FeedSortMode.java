package com.pikume.back.feed.application.dto;

import com.pikume.back.feed.application.exception.InvalidFeedSortException;

public enum FeedSortMode {
	RECOMMENDED("recommended"),
	LATEST("latest");

	private final String apiValue;

	FeedSortMode(String apiValue) {
		this.apiValue = apiValue;
	}

	public String apiValue() {
		return apiValue;
	}

	public static FeedSortMode from(String value) {
		if (value == null || value.isBlank()) {
			return RECOMMENDED;
		}
		for (FeedSortMode mode : values()) {
			if (mode.apiValue.equalsIgnoreCase(value.trim())) {
				return mode;
			}
		}
		throw new InvalidFeedSortException();
	}
}
