package com.pikume.back.creative.application.service;

import com.pikume.back.creative.application.dto.AiGenerationQuotaConsumption;
import com.pikume.back.creative.application.port.in.ConsumeAiGenerationQuotaUseCase;
import com.pikume.back.creative.application.port.in.QueryAiGenerationQuotaUseCase;
import com.pikume.back.creative.application.port.out.AiGenerationQuotaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiGenerationQuotaService
		implements ConsumeAiGenerationQuotaUseCase, QueryAiGenerationQuotaUseCase {

	private static final String AI_GENERATE_QUOTA_NAME = "ai_generate";
	private static final int MAX_AI_GENERATION_REQUESTS_PER_DAY = 3;

	private final AiGenerationQuotaPort aiGenerationQuotaPort;

	@Override
	public AiGenerationQuotaConsumption tryConsumeForGeneration(String userId) {
		Optional<Integer> usageCount = aiGenerationQuotaPort.consumeIfAvailable(
				AI_GENERATE_QUOTA_NAME,
				userId,
				MAX_AI_GENERATION_REQUESTS_PER_DAY);
		return usageCount
				.map(count -> new AiGenerationQuotaConsumption(
						true,
						MAX_AI_GENERATION_REQUESTS_PER_DAY,
						remainingCount(count)))
				.orElseGet(() -> new AiGenerationQuotaConsumption(
						false,
						MAX_AI_GENERATION_REQUESTS_PER_DAY,
						0));
	}

	@Override
	public void releaseGenerationConsumption(String userId) {
		aiGenerationQuotaPort.releaseConsumption(AI_GENERATE_QUOTA_NAME, userId);
	}

	@Override
	public int getRemainingGenerationCount(String userId) {
		return remainingCount(aiGenerationQuotaPort.getUsageCount(AI_GENERATE_QUOTA_NAME, userId));
	}

	@Override
	public int getDailyGenerationLimit() {
		return MAX_AI_GENERATION_REQUESTS_PER_DAY;
	}

	private int remainingCount(int usageCount) {
		return Math.max(0, MAX_AI_GENERATION_REQUESTS_PER_DAY - usageCount);
	}
}
