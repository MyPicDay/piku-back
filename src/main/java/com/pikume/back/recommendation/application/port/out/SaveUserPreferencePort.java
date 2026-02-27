package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.domain.UserPreference;

/**
 * 사용자 선호도 저장 Outbound Port
 */
public interface SaveUserPreferencePort {

	UserPreference save(UserPreference preference);
}
