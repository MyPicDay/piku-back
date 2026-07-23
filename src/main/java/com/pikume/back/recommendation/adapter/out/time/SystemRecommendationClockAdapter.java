package com.pikume.back.recommendation.adapter.out.time;

import com.pikume.back.recommendation.application.port.out.RecommendationClockPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SystemRecommendationClockAdapter implements RecommendationClockPort {

	@Override
	public LocalDateTime now() {
		return LocalDateTime.now();
	}
}
