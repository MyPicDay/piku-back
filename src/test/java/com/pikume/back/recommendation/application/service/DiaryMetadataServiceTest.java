package com.pikume.back.recommendation.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.recommendation.application.dto.DiaryMetadataResult;
import com.pikume.back.recommendation.application.port.out.ContentAnalyzerPort;
import com.pikume.back.recommendation.application.port.out.LoadDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.SaveDiaryMetadataPort;
import com.pikume.back.recommendation.domain.DiaryMetadata;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiaryMetadataServiceTest {

	@InjectMocks
	private DiaryMetadataService diaryMetadataService;

	@Mock
	private ContentAnalyzerPort contentAnalyzerPort;

	@Mock
	private LoadDiaryMetadataPort loadDiaryMetadataPort;

	@Mock
	private SaveDiaryMetadataPort saveDiaryMetadataPort;

	@Nested
	@DisplayName("analyzeAndSave - 일기 분석 및 저장")
	class AnalyzeAndSave {

		@Test
		@DisplayName("신규 일기인 경우 분석 후 저장한다")
		void savesNewMetadata() {
			Long diaryId = 1L;
			String content = "오늘 제주도 여행을 갔다";
			DiaryMetadata analysis = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("travel")
					.topics("{\"travel\":0.6}")
					.qualityScore(0.5)
					.build();

			given(contentAnalyzerPort.analyze(diaryId, content)).willReturn(analysis);
			given(loadDiaryMetadataPort.findByDiaryId(diaryId)).willReturn(Optional.empty());
			given(saveDiaryMetadataPort.save(analysis)).willReturn(analysis);

			diaryMetadataService.analyzeAndSave(diaryId, content);

			verify(saveDiaryMetadataPort).save(analysis);
		}

		@Test
		@DisplayName("기존 메타데이터가 있으면 업데이트한다")
		void updatesExistingMetadata() {
			Long diaryId = 1L;
			String content = "오늘 맛집 탐방";
			DiaryMetadata existing = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("travel")
					.topics("{\"travel\":0.6}")
					.qualityScore(0.5)
					.build();
			DiaryMetadata newAnalysis = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("food")
					.topics("{\"food\":0.6}")
					.qualityScore(0.6)
					.build();

			given(contentAnalyzerPort.analyze(diaryId, content)).willReturn(newAnalysis);
			given(loadDiaryMetadataPort.findByDiaryId(diaryId)).willReturn(Optional.of(existing));

			diaryMetadataService.analyzeAndSave(diaryId, content);
			assertThat(existing.getPrimaryTopic()).isEqualTo("food");
		}
	}

	@Nested
	@DisplayName("getMetadata - 메타데이터 조회")
	class GetMetadata {

		@Test
		@DisplayName("존재하는 메타데이터를 반환한다")
		void returnsMetadata() {
			Long diaryId = 1L;
			DiaryMetadata metadata = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("travel")
					.qualityScore(0.8)
					.build();

			given(loadDiaryMetadataPort.findByDiaryId(diaryId)).willReturn(Optional.of(metadata));

			Optional<DiaryMetadataResult> result = diaryMetadataService.getMetadata(diaryId);

			assertThat(result).isPresent();
			assertThat(result.get().primaryTopic()).isEqualTo("travel");
		}

		@Test
		@DisplayName("존재하지 않으면 빈 Optional을 반환한다")
		void returnsEmptyForMissing() {
			given(loadDiaryMetadataPort.findByDiaryId(99L)).willReturn(Optional.empty());

			Optional<DiaryMetadataResult> result = diaryMetadataService.getMetadata(99L);

			assertThat(result).isEmpty();
		}
	}
}
