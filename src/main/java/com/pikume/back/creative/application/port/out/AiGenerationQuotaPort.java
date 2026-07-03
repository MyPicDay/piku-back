package com.pikume.back.creative.application.port.out;

import java.util.Optional;

public interface AiGenerationQuotaPort {

	int getUsageCount(String quotaName, String userId);

	Optional<Integer> consumeIfAvailable(String quotaName, String userId, int limit);

	void releaseConsumption(String quotaName, String userId);
}
