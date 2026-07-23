package com.pikume.back.creative.adapter.out.crosscontext;

import com.pikume.back.admin.application.port.in.RecordAiPhotoStatisticsEventUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatisticsAdapterForCreative")
class AdminStatisticsAdapterForCreativeTest {

	@Mock
	private RecordAiPhotoStatisticsEventUseCase recordAiPhotoStatisticsEventUseCase;

	@Test
	@DisplayName("creative AI 사진 통계 이벤트를 admin 통계 이벤트로 매핑한다")
	void mapsCreativeStatisticsEventsToAdminStatisticsEvents() {
		AdminStatisticsAdapterForCreative adapter =
				new AdminStatisticsAdapterForCreative(recordAiPhotoStatisticsEventUseCase);

		adapter.recordRequest("user1");
		adapter.recordSuccess("user1");
		adapter.recordFailure("user1");

		then(recordAiPhotoStatisticsEventUseCase).should().recordAiPhotoRequest("user1");
		then(recordAiPhotoStatisticsEventUseCase).should().recordAiPhotoSuccess("user1");
		then(recordAiPhotoStatisticsEventUseCase).should().recordAiPhotoFailure("user1");
	}
}
