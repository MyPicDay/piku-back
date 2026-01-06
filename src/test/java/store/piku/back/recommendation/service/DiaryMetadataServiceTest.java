package store.piku.back.recommendation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.recommendation.entity.DiaryMetadata;
import store.piku.back.recommendation.repository.DiaryMetadataRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryMetadataServiceTest {

	@InjectMocks
	private DiaryMetadataService diaryMetadataService;

	@Mock
	private DiaryMetadataRepository diaryMetadataRepository;

	@Mock
	private LocalContentAnalyzer localContentAnalyzer;

	@Nested
	@DisplayName("analyzeAndSave - 일기 분석 및 저장")
	class AnalyzeAndSave {

		@Test
		@DisplayName("새 일기에 대해 분석 결과를 저장한다")
		void analyzeNewDiary() {
			Long diaryId = 1L;
			String content = "오늘 여행을 다녀왔다.";
			DiaryMetadata metadata = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("travel")
					.topics("{\"travel\":0.9}")
					.qualityScore(0.5)
					.build();

			given(diaryMetadataRepository.findByDiaryId(diaryId)).willReturn(Optional.empty());
			given(localContentAnalyzer.analyze(diaryId, content)).willReturn(metadata);
			given(diaryMetadataRepository.save(any())).willReturn(metadata);

			DiaryMetadata result = diaryMetadataService.analyzeAndSave(diaryId, content);

			assertThat(result.getPrimaryTopic()).isEqualTo("travel");
			verify(diaryMetadataRepository).save(any());
		}

		@Test
		@DisplayName("기존 메타데이터가 있으면 업데이트한다")
		void updateExistingMetadata() {
			Long diaryId = 1L;
			String content = "맛집에서 맛있는 음식을 먹었다.";
			DiaryMetadata existingMetadata = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("travel")
					.topics("{\"travel\":0.6}")
					.qualityScore(0.4)
					.build();
			DiaryMetadata newAnalysis = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("food")
					.topics("{\"food\":0.9}")
					.qualityScore(0.6)
					.build();

			given(diaryMetadataRepository.findByDiaryId(diaryId)).willReturn(Optional.of(existingMetadata));
			given(localContentAnalyzer.analyze(diaryId, content)).willReturn(newAnalysis);

			DiaryMetadata result = diaryMetadataService.analyzeAndSave(diaryId, content);

			assertThat(result.getPrimaryTopic()).isEqualTo("food");
			verify(diaryMetadataRepository, never()).save(any());
		}

		@Test
		@DisplayName("빈 내용도 분석하고 저장한다")
		void analyzeEmptyContent() {
			Long diaryId = 1L;
			String content = "";
			DiaryMetadata metadata = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("daily")
					.topics("{\"daily\":1.0}")
					.qualityScore(0.1)
					.build();

			given(diaryMetadataRepository.findByDiaryId(diaryId)).willReturn(Optional.empty());
			given(localContentAnalyzer.analyze(diaryId, content)).willReturn(metadata);
			given(diaryMetadataRepository.save(any())).willReturn(metadata);

			DiaryMetadata result = diaryMetadataService.analyzeAndSave(diaryId, content);

			assertThat(result.getPrimaryTopic()).isEqualTo("daily");
		}
	}

	@Nested
	@DisplayName("getMetadata - 메타데이터 조회")
	class GetMetadata {

		@Test
		@DisplayName("존재하는 메타데이터를 조회한다")
		void getExistingMetadata() {
			Long diaryId = 1L;
			DiaryMetadata metadata = DiaryMetadata.builder()
					.diaryId(diaryId)
					.primaryTopic("travel")
					.build();

			given(diaryMetadataRepository.findByDiaryId(diaryId)).willReturn(Optional.of(metadata));

			Optional<DiaryMetadata> result = diaryMetadataService.getMetadata(diaryId);

			assertThat(result).isPresent();
			assertThat(result.get().getPrimaryTopic()).isEqualTo("travel");
		}

		@Test
		@DisplayName("존재하지 않는 메타데이터는 empty를 반환한다")
		void getMissingMetadata() {
			given(diaryMetadataRepository.findByDiaryId(999L)).willReturn(Optional.empty());

			Optional<DiaryMetadata> result = diaryMetadataService.getMetadata(999L);

			assertThat(result).isEmpty();
		}
	}
}
