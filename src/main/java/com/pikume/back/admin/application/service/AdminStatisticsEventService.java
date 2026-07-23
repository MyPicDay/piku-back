package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.in.RecordAdminStatisticsEventUseCase;
import com.pikume.back.admin.application.port.in.RecordAiPhotoStatisticsEventUseCase;
import com.pikume.back.admin.application.port.out.SaveAdminStatisticsEventPort;
import com.pikume.back.admin.domain.AdminStatisticsEvent;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AdminStatisticsEventService
		implements RecordAdminStatisticsEventUseCase, RecordAiPhotoStatisticsEventUseCase {

	private static final ZoneId STATISTICS_ZONE = ZoneId.of("Asia/Seoul");

	private final SaveAdminStatisticsEventPort saveAdminStatisticsEventPort;

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(AdminStatisticsEventType eventType, String userId, String visitorKey) {
		saveAdminStatisticsEventPort.save(AdminStatisticsEvent.record(
				eventType,
				LocalDateTime.now(STATISTICS_ZONE),
				userId,
				visitorKey));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordAiPhotoRequest(String userId) {
		record(AdminStatisticsEventType.AI_PHOTO_REQUEST, userId, null);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordAiPhotoSuccess(String userId) {
		record(AdminStatisticsEventType.AI_PHOTO_SUCCESS, userId, null);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordAiPhotoFailure(String userId) {
		record(AdminStatisticsEventType.AI_PHOTO_FAILURE, userId, null);
	}
}
