package com.pikume.back.recommendation.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TopicAffinities {

	private static final double MAX_AFFINITY = 1.0;

	private final Map<String, Double> values;

	private TopicAffinities(Map<String, Double> values) {
		this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}

	public static TopicAffinities empty() {
		return new TopicAffinities(Map.of());
	}

	public static TopicAffinities from(Map<String, Double> values) {
		return values == null || values.isEmpty()
				? empty()
				: new TopicAffinities(values);
	}

	public TopicAffinities record(String topic, InteractionType interactionType) {
		Map<String, Double> updated = new LinkedHashMap<>(values);
		double currentValue = updated.getOrDefault(topic, 0.0);
		double newValue = Math.min(MAX_AFFINITY, currentValue + interactionType.weight());
		updated.put(topic, newValue);
		return new TopicAffinities(updated);
	}

	public double scoreFor(String topic, double defaultValue) {
		return values.getOrDefault(topic, defaultValue);
	}

	public Map<String, Double> values() {
		return values;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TopicAffinities that)) {
			return false;
		}
		return values.equals(that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(values);
	}
}
