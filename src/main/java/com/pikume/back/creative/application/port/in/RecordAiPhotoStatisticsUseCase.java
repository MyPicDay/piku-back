package com.pikume.back.creative.application.port.in;

public interface RecordAiPhotoStatisticsUseCase {

	void recordRequest(String userId);

	void recordSuccess(String userId);

	void recordFailure(String userId);
}
