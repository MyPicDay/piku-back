package com.pikume.back.diary.adapter.in.scheduler;

import com.pikume.back.diary.application.service.PhotoOptimizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "photo.optimization", name = "enabled", havingValue = "true")
public class PhotoOptimizationScheduler {

	private final PhotoOptimizationService photoOptimizationService;

	@Scheduled(fixedDelayString = "${photo.optimization.fixed-delay-ms:300000}")
	public void run() {
		photoOptimizationService.optimizePendingPhotos();
	}
}
