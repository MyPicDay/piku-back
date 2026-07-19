package com.pikume.back.feed.adapter.out.crosscontext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.recommendation.application.dto.DiaryMetadataResult;
import com.pikume.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import com.pikume.back.recommendation.application.port.in.ManageUserPreferenceUseCase;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationAdapterForFeed")
class RecommendationAdapterForFeedTest {

	@InjectMocks
	private RecommendationAdapterForFeed adapter;

	@Mock
	private ManageUserPreferenceUseCase manageUserPreferenceUseCase;
	@Mock
	private AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;

	@Nested
	@DisplayName("recordClickPreference")
	class RecordClickPreference {

		@Test
		@DisplayName("일기 메타데이터 주제를 CLICK 상호작용으로 번역한다")
		void translatesMetadataTopicToClickInteraction() {
			given(analyzeDiaryContentUseCase.getMetadata(1L))
					.willReturn(Optional.of(new DiaryMetadataResult(1L, "travel", "travel", 0.8)));

			adapter.recordClickPreference("user-id", 1L);

			then(manageUserPreferenceUseCase).should()
					.recordInteraction("user-id", "travel", "CLICK");
		}

		@Test
		@DisplayName("메타데이터가 없으면 daily 주제로 CLICK 상호작용을 기록한다")
		void usesDailyWhenMetadataIsMissing() {
			given(analyzeDiaryContentUseCase.getMetadata(1L)).willReturn(Optional.empty());

			adapter.recordClickPreference("user-id", 1L);

			then(manageUserPreferenceUseCase).should()
					.recordInteraction("user-id", "daily", "CLICK");
		}
	}
}
