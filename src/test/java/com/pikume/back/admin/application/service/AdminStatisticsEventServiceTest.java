package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.port.out.SaveAdminStatisticsEventPort;
import com.pikume.back.admin.domain.AdminStatisticsEvent;
import com.pikume.back.admin.domain.AdminStatisticsEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatisticsEventService")
class AdminStatisticsEventServiceTest {

	@Mock
	private SaveAdminStatisticsEventPort saveAdminStatisticsEventPort;

	@Test
	@DisplayName("Creative의 통계 기록 의도를 Admin 통계 이벤트로 번역한다")
	void recordsAiPhotoStatisticsEvents() {
		AdminStatisticsEventService service = new AdminStatisticsEventService(saveAdminStatisticsEventPort);

		service.recordAiPhotoRequest("user-1");
		service.recordAiPhotoSuccess("user-1");
		service.recordAiPhotoFailure("user-1");

		ArgumentCaptor<AdminStatisticsEvent> eventCaptor = ArgumentCaptor.forClass(AdminStatisticsEvent.class);
		then(saveAdminStatisticsEventPort).should(times(3)).save(eventCaptor.capture());
		assertThat(eventCaptor.getAllValues())
				.extracting(AdminStatisticsEvent::getEventType)
				.containsExactly(
						AdminStatisticsEventType.AI_PHOTO_REQUEST,
						AdminStatisticsEventType.AI_PHOTO_SUCCESS,
						AdminStatisticsEventType.AI_PHOTO_FAILURE);
		assertThat(eventCaptor.getAllValues())
				.extracting(AdminStatisticsEvent::getUserId)
				.containsOnly("user-1");
		assertThat(eventCaptor.getAllValues())
				.extracting(AdminStatisticsEvent::getVisitorKey)
				.containsOnlyNulls();
	}
}
