package com.pikume.back.recommendation.adapter.out.analyzer;

import com.pikume.back.recommendation.application.dto.DiaryContentAnalysis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalContentAnalyzerAdapter")
class LocalContentAnalyzerAdapterTest {

	private final LocalContentAnalyzerAdapter adapter = new LocalContentAnalyzerAdapter();

	@Test
	@DisplayName("기존 키워드와 길이 정책을 기술 중립 분석 결과로 반환한다")
	void analyzesWithCurrentKeywordAndQualityPolicy() {
		DiaryContentAnalysis analysis =
				adapter.analyze("제주 공항에서 여행을 시작하고 아주 즐거운 하루를 보냈다");

		assertThat(analysis.primaryTopic()).isEqualTo("travel");
		assertThat(analysis.topicScores()).containsEntry("travel", 0.9);
		assertThat(analysis.qualityScore()).isEqualTo(0.3);
	}

	@Test
	@DisplayName("내용이 비어 있으면 daily 기본 주제와 0.1 품질을 사용한다")
	void usesDailyDefaultsForBlankContent() {
		DiaryContentAnalysis analysis = adapter.analyze(" ");

		assertThat(analysis.primaryTopic()).isEqualTo("daily");
		assertThat(analysis.topicScores()).containsExactlyEntriesOf(java.util.Map.of("daily", 1.0));
		assertThat(analysis.qualityScore()).isEqualTo(0.1);
	}

}
