package com.pikume.back.recommendation.application.port.in;

import java.util.Map;

public interface QueryUserTopicAffinitiesUseCase {

	Map<String, Double> queryUserTopicAffinities(String userId);
}
