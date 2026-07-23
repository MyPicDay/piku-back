package com.pikume.back.recommendation.application.port.out;

import java.time.LocalDateTime;

public interface RecommendationClockPort {

	LocalDateTime now();
}
