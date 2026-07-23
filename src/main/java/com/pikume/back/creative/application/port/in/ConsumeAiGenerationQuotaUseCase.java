package com.pikume.back.creative.application.port.in;

import com.pikume.back.creative.application.dto.AiGenerationQuotaConsumption;

public interface ConsumeAiGenerationQuotaUseCase {

	AiGenerationQuotaConsumption tryConsumeForGeneration(String userId);

	void releaseGenerationConsumption(String userId);
}
