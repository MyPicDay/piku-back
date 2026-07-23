package com.pikume.back.recommendation.application.service;

import com.pikume.back.recommendation.application.dto.DiaryContentAnalysis;
import com.pikume.back.recommendation.application.dto.DiaryMetadataResult;
import com.pikume.back.recommendation.application.port.out.ContentAnalyzerPort;
import com.pikume.back.recommendation.application.port.out.LoadDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.RecordDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.RecommendationClockPort;
import com.pikume.back.recommendation.domain.DiaryMetadata;
import com.pikume.back.recommendation.domain.TopicScores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryMetadataService")
class DiaryMetadataServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 23, 12, 0);

	@Mock
	private ContentAnalyzerPort contentAnalyzerPort;
	@Mock
	private LoadDiaryMetadataPort loadDiaryMetadataPort;
	@Mock
	private RecordDiaryMetadataPort recordDiaryMetadataPort;
	@Mock
	private RecommendationClockPort recommendationClockPort;

	private DiaryMetadataService diaryMetadataService;

	@BeforeEach
	void setUp() {
		diaryMetadataService = new DiaryMetadataService(
				contentAnalyzerPort,
				loadDiaryMetadataPort,
				recordDiaryMetadataPort,
				recommendationClockPort);
	}

	@Nested
	@DisplayName("analyzeDiaryContent")
	class AnalyzeDiaryContent {

		@Test
		@DisplayName("신규 일기는 기술 중립 분석 결과를 메타데이터로 기록한다")
		void recordsNewMetadata() {
			DiaryContentAnalysis analysis =
					new DiaryContentAnalysis("travel", Map.of("travel", 0.6), 0.5);
			given(contentAnalyzerPort.analyze("오늘 제주도 여행을 갔다")).willReturn(analysis);
			given(loadDiaryMetadataPort.loadByDiaryId(1L)).willReturn(Optional.empty());
			given(recommendationClockPort.now()).willReturn(NOW);

			diaryMetadataService.analyzeDiaryContent(1L, "오늘 제주도 여행을 갔다");

			then(recordDiaryMetadataPort).should().recordDiaryMetadata(
					org.mockito.ArgumentMatchers.argThat(metadata ->
							metadata.getDiaryId().equals(1L)
									&& metadata.getPrimaryTopic().equals("travel")
									&& metadata.getTopics().values().equals(Map.of("travel", 0.6))
									&& metadata.getAnalyzedAt().equals(NOW)));
		}

		@Test
		@DisplayName("기존 메타데이터는 새 분석 결과와 주입된 시각으로 갱신한다")
		void updatesExistingMetadata() {
			DiaryMetadata existing = DiaryMetadata.create(
					1L,
					"travel",
					TopicScores.from(Map.of("travel", 0.6)),
					0.5,
					NOW.minusDays(1));
			DiaryContentAnalysis analysis =
					new DiaryContentAnalysis("food", Map.of("food", 0.6), 0.6);
			given(contentAnalyzerPort.analyze("오늘 맛집 탐방")).willReturn(analysis);
			given(loadDiaryMetadataPort.loadByDiaryId(1L)).willReturn(Optional.of(existing));
			given(recommendationClockPort.now()).willReturn(NOW);

			diaryMetadataService.analyzeDiaryContent(1L, "오늘 맛집 탐방");

			assertThat(existing.getPrimaryTopic()).isEqualTo("food");
			assertThat(existing.getTopics().values()).containsEntry("food", 0.6);
			assertThat(existing.getAnalyzedAt()).isEqualTo(NOW);
			then(recordDiaryMetadataPort).should(never()).recordDiaryMetadata(existing);
		}
	}

	@Nested
	@DisplayName("queryDiaryMetadata")
	class QueryDiaryMetadata {

		@Test
		@DisplayName("Domain Entity 대신 공개 메타데이터 결과를 반환한다")
		void returnsMetadataResult() {
			DiaryMetadata metadata = DiaryMetadata.create(
					1L,
					"travel",
					TopicScores.from(Map.of("travel", 0.6)),
					0.8,
					NOW);
			given(loadDiaryMetadataPort.loadByDiaryId(1L)).willReturn(Optional.of(metadata));

			Optional<DiaryMetadataResult> result = diaryMetadataService.queryDiaryMetadata(1L);

			assertThat(result).contains(new DiaryMetadataResult(
					1L,
					"travel",
					Map.of("travel", 0.6),
					0.8));
		}

		@Test
		@DisplayName("메타데이터가 없으면 빈 Optional을 반환한다")
		void returnsEmptyForMissing() {
			given(loadDiaryMetadataPort.loadByDiaryId(99L)).willReturn(Optional.empty());

			assertThat(diaryMetadataService.queryDiaryMetadata(99L)).isEmpty();
		}
	}
}
