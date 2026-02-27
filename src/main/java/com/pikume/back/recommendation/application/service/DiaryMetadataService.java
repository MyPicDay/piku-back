package com.pikume.back.recommendation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pikume.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import com.pikume.back.recommendation.application.port.out.ContentAnalyzerPort;
import com.pikume.back.recommendation.application.port.out.LoadDiaryMetadataPort;
import com.pikume.back.recommendation.application.port.out.SaveDiaryMetadataPort;
import com.pikume.back.recommendation.domain.DiaryMetadata;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryMetadataService implements AnalyzeDiaryContentUseCase {

	private final ContentAnalyzerPort contentAnalyzerPort;
	private final LoadDiaryMetadataPort loadDiaryMetadataPort;
	private final SaveDiaryMetadataPort saveDiaryMetadataPort;

	@Override
	@Transactional
	public DiaryMetadata analyzeAndSave(Long diaryId, String content) {
		DiaryMetadata analysis = contentAnalyzerPort.analyze(diaryId, content);

		Optional<DiaryMetadata> existing = loadDiaryMetadataPort.findByDiaryId(diaryId);

		if (existing.isPresent()) {
			DiaryMetadata metadata = existing.get();
			metadata.updateAnalysis(
					analysis.getPrimaryTopic(),
					analysis.getTopics(),
					analysis.getQualityScore());
			log.debug("일기 메타데이터 업데이트 - diaryId: {}, topic: {}", diaryId, analysis.getPrimaryTopic());
			return metadata;
		} else {
			log.debug("일기 메타데이터 신규 저장 - diaryId: {}, topic: {}", diaryId, analysis.getPrimaryTopic());
			return saveDiaryMetadataPort.save(analysis);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DiaryMetadata> getMetadata(Long diaryId) {
		return loadDiaryMetadataPort.findByDiaryId(diaryId);
	}
}
