package com.pikume.back.feed.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.port.out.RecordFeedClickPreferencePort;
import com.pikume.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import com.pikume.back.recommendation.application.port.in.ManageUserPreferenceUseCase;

@Component
@RequiredArgsConstructor
public class RecommendationAdapterForFeed implements RecordFeedClickPreferencePort {

	private final ManageUserPreferenceUseCase manageUserPreferenceUseCase;
	private final AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;

	@Override
	public void recordClickPreference(String userId, Long diaryId) {
		String topic = analyzeDiaryContentUseCase.getMetadata(diaryId)
				.map(metadata -> metadata.primaryTopic())
				.orElse("daily");
		manageUserPreferenceUseCase.recordInteraction(userId, topic, "CLICK");
	}
}
