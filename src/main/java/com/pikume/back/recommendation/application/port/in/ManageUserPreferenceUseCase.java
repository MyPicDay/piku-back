package com.pikume.back.recommendation.application.port.in;

import com.pikume.back.recommendation.domain.UserPreference;

import java.util.Map;
import java.util.Optional;

/**
 * 사용자 선호도 관리 Inbound Port
 */
public interface ManageUserPreferenceUseCase {

	UserPreference updatePreference(String userId, String topic, double weight);

	Optional<UserPreference> getPreference(String userId);

	void recordInteraction(String userId, String topic, String interactionType);

	Map<String, Double> parseAffinities(String json);
}
