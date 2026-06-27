package com.pikume.back.creative.application.port.out;

public interface RecordAiPhotoStatisticsPort {

	void recordRequest(String userId);

	void recordSuccess(String userId);

	void recordFailure(String userId);
}
