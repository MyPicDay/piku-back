package store.piku.back.recommendation.application.port.out;

import store.piku.back.recommendation.domain.UserPreference;

import java.util.Optional;

/**
 * 사용자 선호도 조회 Outbound Port
 */
public interface LoadUserPreferencePort {

	Optional<UserPreference> findByUserId(String userId);

	boolean existsByUserId(String userId);
}
