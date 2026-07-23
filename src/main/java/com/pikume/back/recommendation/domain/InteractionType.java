package com.pikume.back.recommendation.domain;

import java.util.Locale;

public enum InteractionType {
	LIKE(0.3),
	VIEW(0.1),
	CLICK(0.15),
	OTHER(0.05);

	private final double weight;

	InteractionType(double weight) {
		this.weight = weight;
	}

	public double weight() {
		return weight;
	}

	public static InteractionType from(String value) {
		if (value == null) {
			return OTHER;
		}
		try {
			return valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return OTHER;
		}
	}
}
