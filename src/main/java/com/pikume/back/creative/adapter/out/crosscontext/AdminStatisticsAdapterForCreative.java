package com.pikume.back.creative.adapter.out.crosscontext;

import com.pikume.back.admin.application.port.in.RecordAiPhotoStatisticsEventUseCase;
import com.pikume.back.creative.application.port.out.RecordAiPhotoStatisticsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminStatisticsAdapterForCreative implements RecordAiPhotoStatisticsPort {

	private final RecordAiPhotoStatisticsEventUseCase recordAiPhotoStatisticsEventUseCase;

	@Override
	public void recordRequest(String userId) {
		recordAiPhotoStatisticsEventUseCase.recordAiPhotoRequest(userId);
	}

	@Override
	public void recordSuccess(String userId) {
		recordAiPhotoStatisticsEventUseCase.recordAiPhotoSuccess(userId);
	}

	@Override
	public void recordFailure(String userId) {
		recordAiPhotoStatisticsEventUseCase.recordAiPhotoFailure(userId);
	}
}
