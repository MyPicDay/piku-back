package com.pikume.back.admin.application.port.in;

public interface RecordAiPhotoStatisticsEventUseCase {

	void recordAiPhotoRequest(String userId);

	void recordAiPhotoSuccess(String userId);

	void recordAiPhotoFailure(String userId);
}
