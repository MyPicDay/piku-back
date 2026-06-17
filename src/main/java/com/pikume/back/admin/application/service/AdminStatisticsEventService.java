package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.in.RecordAdminStatisticsEventUseCase;
import com.pikume.back.admin.application.port.out.SaveAdminStatisticsEventPort;
import com.pikume.back.admin.domain.AdminStatisticsEvent;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminStatisticsEventService implements RecordAdminStatisticsEventUseCase {

	private final SaveAdminStatisticsEventPort saveAdminStatisticsEventPort;

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(AdminStatisticsEventType eventType, String userId, String visitorKey) {
		saveAdminStatisticsEventPort.save(AdminStatisticsEvent.record(
				eventType,
				LocalDateTime.now(),
				userId,
				visitorKey));
	}
}
