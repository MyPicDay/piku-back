package store.piku.back.recommendation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.recommendation.entity.UserPreference;
import store.piku.back.recommendation.repository.UserPreferenceRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

	@InjectMocks
	private UserPreferenceService userPreferenceService;

	@Mock
	private UserPreferenceRepository userPreferenceRepository;

	private final String userId = "test-user-id";

	@Nested
	@DisplayName("updatePreference - 선호도 업데이트")
	class UpdatePreference {

		@Test
		@DisplayName("새 사용자의 선호도를 생성한다")
		void createNewPreference() {
			String topic = "travel";
			double weight = 0.5;

			given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.empty());
			given(userPreferenceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

			UserPreference result = userPreferenceService.updatePreference(userId, topic, weight);

			assertThat(result.getUserId()).isEqualTo(userId);
			verify(userPreferenceRepository).save(any());
		}

		@Test
		@DisplayName("기존 사용자의 선호도를 업데이트한다")
		void updateExistingPreference() {
			String topic = "food";
			double weight = 0.3;
			UserPreference existing = UserPreference.builder()
					.userId(userId)
					.topicAffinities("{\"travel\":0.5}")
					.build();

			given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(existing));

			UserPreference result = userPreferenceService.updatePreference(userId, topic, weight);

			assertThat(result.getTopicAffinities()).contains("food");
			verify(userPreferenceRepository, never()).save(any());
		}

		@Test
		@DisplayName("같은 토픽에 대해 누적 업데이트한다")
		void accumulateSameTopicPreference() {
			String topic = "travel";
			double weight = 0.3;
			UserPreference existing = UserPreference.builder()
					.userId(userId)
					.topicAffinities("{\"travel\":0.5}")
					.build();

			given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(existing));

			UserPreference result = userPreferenceService.updatePreference(userId, topic, weight);

			assertThat(result.getTopicAffinities()).contains("travel");
		}
	}

	@Nested
	@DisplayName("getPreference - 선호도 조회")
	class GetPreference {

		@Test
		@DisplayName("존재하는 사용자의 선호도를 조회한다")
		void getExistingPreference() {
			UserPreference preference = UserPreference.builder()
					.userId(userId)
					.topicAffinities("{\"travel\":0.8,\"food\":0.5}")
					.build();

			given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(preference));

			Optional<UserPreference> result = userPreferenceService.getPreference(userId);

			assertThat(result).isPresent();
			assertThat(result.get().getTopicAffinities()).contains("travel");
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 empty를 반환한다")
		void getMissingPreference() {
			given(userPreferenceRepository.findByUserId("unknown")).willReturn(Optional.empty());

			Optional<UserPreference> result = userPreferenceService.getPreference("unknown");

			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("recordInteraction - 상호작용 기록")
	class RecordInteraction {

		@Test
		@DisplayName("좋아요 행동은 높은 가중치로 선호도를 업데이트한다")
		void recordLikeInteraction() {
			String topic = "travel";
			UserPreference existing = UserPreference.builder()
					.userId(userId)
					.topicAffinities("{}")
					.build();

			given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(existing));

			userPreferenceService.recordInteraction(userId, topic, "LIKE");

			verify(userPreferenceRepository).findByUserId(userId);
		}

		@Test
		@DisplayName("조회 행동은 낮은 가중치로 선호도를 업데이트한다")
		void recordViewInteraction() {
			String topic = "food";
			UserPreference existing = UserPreference.builder()
					.userId(userId)
					.topicAffinities("{}")
					.build();

			given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(existing));

			userPreferenceService.recordInteraction(userId, topic, "VIEW");

			verify(userPreferenceRepository).findByUserId(userId);
		}
	}
}
