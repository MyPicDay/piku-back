package com.pikume.back.recommendation.application.service;

import com.pikume.back.recommendation.application.port.out.LoadUserPreferencePort;
import com.pikume.back.recommendation.application.port.out.RecordUserPreferencePort;
import com.pikume.back.recommendation.application.port.out.RecommendationClockPort;
import com.pikume.back.recommendation.domain.UserPreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferenceService")
class UserPreferenceServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 23, 12, 0);

	@Mock
	private LoadUserPreferencePort loadUserPreferencePort;
	@Mock
	private RecordUserPreferencePort recordUserPreferencePort;
	@Mock
	private RecommendationClockPort recommendationClockPort;

	private UserPreferenceService service;

	@BeforeEach
	void setUp() {
		service = new UserPreferenceService(
				loadUserPreferencePort,
				recordUserPreferencePort,
				recommendationClockPort);
	}

	@Test
	@DisplayName("CLICK 의도를 현재 0.15 가중치로 기록한다")
	void recordsClickWeight() {
		UserPreference preference = UserPreference.create("user-1", NOW.minusDays(1));
		given(loadUserPreferencePort.loadByUserId("user-1")).willReturn(Optional.of(preference));
		given(recommendationClockPort.now()).willReturn(NOW);

		service.recordClick("user-1", "travel");

		assertThat(preference.getTopicAffinities().values())
				.containsEntry("travel", 0.15);
		assertThat(preference.getLastUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("선호도가 없으면 빈 선호도를 먼저 기록하고 상호작용을 반영한다")
	void createsPreferenceBeforeRecordingInteraction() {
		given(loadUserPreferencePort.loadByUserId("user-1")).willReturn(Optional.empty());
		given(recommendationClockPort.now()).willReturn(NOW);
		given(recordUserPreferencePort.recordUserPreference(
				org.mockito.ArgumentMatchers.any(UserPreference.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

		service.recordView("user-1", "daily");

		ArgumentCaptor<UserPreference> preferenceCaptor = ArgumentCaptor.forClass(UserPreference.class);
		then(recordUserPreferencePort).should().recordUserPreference(preferenceCaptor.capture());
		assertThat(preferenceCaptor.getValue().getTopicAffinities().values())
				.containsEntry("daily", 0.1);
	}

	@Test
	@DisplayName("사용자 식별자가 없으면 빈 친화도를 반환한다")
	void returnsEmptyAffinitiesForAnonymousUser() {
		assertThat(service.queryUserTopicAffinities(null)).isEqualTo(Map.of());
	}
}
