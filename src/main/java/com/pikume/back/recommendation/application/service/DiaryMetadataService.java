package com.pikume.back.recommendation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.recommendation.application.dto.DiaryContentAnalysis;
import com.pikume.back.recommendation.application.dto.DiaryMetadataResult;
import com.pikume.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import com.pikume.back.recommendation.application.port.in.QueryDiaryMetadataUseCase;
import com.pikume.back.recommendation.application.port.out.ContentAnalyzerPort;
import com.pikume.back.recommendation.application.port.out.LoadDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.RecordDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.RecommendationClockPort;
import com.pikume.back.recommendation.domain.DiaryMetadata;
import com.pikume.back.recommendation.domain.TopicScores;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryMetadataService implements AnalyzeDiaryContentUseCase, QueryDiaryMetadataUseCase {

	private final ContentAnalyzerPort contentAnalyzerPort;
	private final LoadDiaryMetadataPort loadDiaryMetadataPort;
	private final RecordDiaryMetadataPort recordDiaryMetadataPort;
	private final RecommendationClockPort recommendationClockPort;

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void analyzeDiaryContent(Long diaryId, String content) {
		DiaryContentAnalysis analysis = contentAnalyzerPort.analyze(content);
		TopicScores topicScores = TopicScores.from(analysis.topicScores());

		Optional<DiaryMetadata> existing = loadDiaryMetadataPort.loadByDiaryId(diaryId);

		if (existing.isPresent()) {
			DiaryMetadata metadata = existing.get();
			metadata.updateAnalysis(
					analysis.primaryTopic(),
					topicScores,
					analysis.qualityScore(),
					recommendationClockPort.now());
			log.debug("일기 메타데이터 업데이트 - diaryId: {}, topic: {}", diaryId, analysis.primaryTopic());
		} else {
			DiaryMetadata metadata = DiaryMetadata.create(
					diaryId,
					analysis.primaryTopic(),
					topicScores,
					analysis.qualityScore(),
					recommendationClockPort.now());
			log.debug("일기 메타데이터 신규 저장 - diaryId: {}, topic: {}", diaryId, analysis.primaryTopic());
			recordDiaryMetadataPort.recordDiaryMetadata(metadata);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DiaryMetadataResult> queryDiaryMetadata(Long diaryId) {
		return loadDiaryMetadataPort.loadByDiaryId(diaryId)
				.map(metadata -> new DiaryMetadataResult(
						metadata.getDiaryId(),
						metadata.getPrimaryTopic(),
						metadata.getTopics().values(),
						metadata.getQualityScore()));
	}
}
