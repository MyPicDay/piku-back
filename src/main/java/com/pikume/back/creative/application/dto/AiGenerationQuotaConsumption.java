package com.pikume.back.creative.application.dto;

public record AiGenerationQuotaConsumption(
		boolean consumed,
		int dailyLimit,
		int remainingCount) {
}
