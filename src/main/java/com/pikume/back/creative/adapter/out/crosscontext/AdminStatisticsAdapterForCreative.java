package com.pikume.back.creative.adapter.out.crosscontext;

import com.pikume.back.admin.application.port.in.RecordAdminStatisticsEventUseCase;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import com.pikume.back.creative.application.port.out.RecordAiPhotoStatisticsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminStatisticsAdapterForCreative implements RecordAiPhotoStatisticsPort {

	private final RecordAdminStatisticsEventUseCase recordAdminStatisticsEventUseCase;

	@Override
	public void recordRequest(String userId) {
		recordAdminStatisticsEventUseCase.record(AdminStatisticsEventType.AI_PHOTO_REQUEST, userId, null);
	}

	@Override
	public void recordSuccess(String userId) {
		recordAdminStatisticsEventUseCase.record(AdminStatisticsEventType.AI_PHOTO_SUCCESS, userId, null);
	}

	@Override
	public void recordFailure(String userId) {
		recordAdminStatisticsEventUseCase.record(AdminStatisticsEventType.AI_PHOTO_FAILURE, userId, null);
	}
}
