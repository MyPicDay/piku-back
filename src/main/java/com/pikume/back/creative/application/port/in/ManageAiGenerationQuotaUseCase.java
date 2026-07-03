package com.pikume.back.creative.application.port.in;

import com.pikume.back.creative.application.dto.AiGenerationQuotaConsumption;

public interface ManageAiGenerationQuotaUseCase {

	AiGenerationQuotaConsumption tryConsumeForGeneration(String userId);

	void releaseGenerationConsumption(String userId);

	int getRemainingGenerationCount(String userId);

	int getDailyGenerationLimit();
}
