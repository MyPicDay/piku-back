package store.piku.back.recommendation.application.port.out;

import store.piku.back.recommendation.domain.UserPreference;

/**
 * 사용자 선호도 저장 Outbound Port
 */
public interface SaveUserPreferencePort {

	UserPreference save(UserPreference preference);
}
