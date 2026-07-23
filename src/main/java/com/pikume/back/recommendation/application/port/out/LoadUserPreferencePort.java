package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.domain.UserPreference;

import java.util.Optional;

/**
 * 사용자 선호도 조회 Outbound Port
 */
public interface LoadUserPreferencePort {

	Optional<UserPreference> loadByUserId(String userId);
}
