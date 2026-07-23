package com.pikume.back.feed.adapter.out.crosscontext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.port.out.RecordFeedClickPreferencePort;
import com.pikume.back.recommendation.application.port.in.QueryDiaryMetadataUseCase;
import com.pikume.back.recommendation.application.port.in.RecordTopicInteractionUseCase;

@Component
@RequiredArgsConstructor
public class RecommendationAdapterForFeed implements RecordFeedClickPreferencePort {

	private final QueryDiaryMetadataUseCase queryDiaryMetadataUseCase;
	private final RecordTopicInteractionUseCase recordTopicInteractionUseCase;

	@Override
	public void recordClickPreference(String userId, Long diaryId) {
		String topic = queryDiaryMetadataUseCase.queryDiaryMetadata(diaryId)
				.map(metadata -> metadata.primaryTopic())
				.orElse("daily");
		recordTopicInteractionUseCase.recordClick(userId, topic);
	}
}
