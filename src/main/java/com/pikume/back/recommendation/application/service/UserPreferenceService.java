package com.pikume.back.recommendation.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.recommendation.application.port.in.ManageUserPreferenceUseCase;
import com.pikume.back.recommendation.application.port.out.LoadUserPreferencePort;
import com.pikume.back.recommendation.application.port.out.SaveUserPreferencePort;
import com.pikume.back.recommendation.domain.UserPreference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceService implements ManageUserPreferenceUseCase {

	private final LoadUserPreferencePort loadUserPreferencePort;
	private final SaveUserPreferencePort saveUserPreferencePort;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final double LIKE_WEIGHT = 0.3;
	private static final double VIEW_WEIGHT = 0.1;
	private static final double CLICK_WEIGHT = 0.15;
	private static final double MAX_AFFINITY = 1.0;

	@Override
	@Transactional
	public UserPreference updatePreference(String userId, String topic, double weight) {
		UserPreference preference = loadUserPreferencePort.findByUserId(userId)
				.orElseGet(() -> {
					UserPreference newPref = UserPreference.builder()
							.userId(userId)
							.topicAffinities("{}")
							.build();
					return saveUserPreferencePort.save(newPref);
				});

		Map<String, Double> affinities = parseAffinities(preference.getTopicAffinities());
		double currentValue = affinities.getOrDefault(topic, 0.0);
		double newValue = Math.min(MAX_AFFINITY, currentValue + weight);
		affinities.put(topic, newValue);

		preference.updateTopicAffinities(toJson(affinities));
		log.debug("사용자 선호도 업데이트 - userId: {}, topic: {}, affinity: {}", userId, topic, newValue);

		return preference;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UserPreference> getPreference(String userId) {
		return loadUserPreferencePort.findByUserId(userId);
	}

	@Override
	@Transactional
	public void recordInteraction(String userId, String topic, String interactionType) {
		double weight = switch (interactionType.toUpperCase()) {
			case "LIKE" -> LIKE_WEIGHT;
			case "VIEW" -> VIEW_WEIGHT;
			case "CLICK" -> CLICK_WEIGHT;
			default -> 0.05;
		};

		updatePreference(userId, topic, weight);
		log.debug("상호작용 기록 - userId: {}, topic: {}, type: {}, weight: {}",
				userId, topic, interactionType, weight);
	}

	@Override
	public Map<String, Double> parseAffinities(String json) {
		if (json == null || json.isBlank() || json.equals("{}")) {
			return new HashMap<>();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			log.warn("선호도 JSON 파싱 실패: {}", json);
			return new HashMap<>();
		}
	}

	private String toJson(Map<String, Double> affinities) {
		try {
			return objectMapper.writeValueAsString(affinities);
		} catch (JsonProcessingException e) {
			log.error("선호도 JSON 변환 실패");
			return "{}";
		}
	}
}
