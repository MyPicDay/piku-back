package com.pikume.back.recommendation.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TopicScores {

	private final Map<String, Double> values;

	private TopicScores(Map<String, Double> values) {
		this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}

	public static TopicScores empty() {
		return new TopicScores(Map.of());
	}

	public static TopicScores from(Map<String, Double> values) {
		return values == null || values.isEmpty()
				? empty()
				: new TopicScores(values);
	}

	public static TopicScores dailyDefault() {
		return from(Map.of("daily", 1.0));
	}

	public Map<String, Double> values() {
		return values;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TopicScores that)) {
			return false;
		}
		return values.equals(that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(values);
	}
}
