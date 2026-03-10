package com.pikume.back.recommendation.application.port.in;

import java.util.Map;

/**
 * 사용자 선호도 관리 Inbound Port
 */
public interface ManageUserPreferenceUseCase {

	void recordInteraction(String userId, String topic, String interactionType);

	Map<String, Double> getUserAffinities(String userId);

	Map<String, Double> parseAffinities(String json);
}
