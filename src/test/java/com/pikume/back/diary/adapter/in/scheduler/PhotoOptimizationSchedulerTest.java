package com.pikume.back.diary.adapter.in.scheduler;

import com.pikume.back.diary.application.service.PhotoOptimizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("PhotoOptimizationScheduler")
class PhotoOptimizationSchedulerTest {

	@Test
	@DisplayName("스케줄 실행 시 pending photo 최적화를 호출한다")
	void runsPhotoOptimizationService() {
		PhotoOptimizationService service = mock(PhotoOptimizationService.class);
		PhotoOptimizationScheduler scheduler = new PhotoOptimizationScheduler(service);

		scheduler.run();

		then(service).should().optimizePendingPhotos();
	}
}
