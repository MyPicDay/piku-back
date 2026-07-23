package com.pikume.back.creative.application.port.in;

public interface QueryAiGenerationQuotaUseCase {

	int getRemainingGenerationCount(String userId);

	int getDailyGenerationLimit();
}
