package store.piku.back.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.recommendation.entity.DiaryMetadata;
import store.piku.back.recommendation.repository.DiaryMetadataRepository;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryMetadataService {

	private final DiaryMetadataRepository diaryMetadataRepository;
	private final LocalContentAnalyzer localContentAnalyzer;

	@Transactional
	public DiaryMetadata analyzeAndSave(Long diaryId, String content) {
		DiaryMetadata analysis = localContentAnalyzer.analyze(diaryId, content);

		Optional<DiaryMetadata> existing = diaryMetadataRepository.findByDiaryId(diaryId);

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
			return diaryMetadataRepository.save(analysis);
		}
	}

	@Transactional(readOnly = true)
	public Optional<DiaryMetadata> getMetadata(Long diaryId) {
		return diaryMetadataRepository.findByDiaryId(diaryId);
	}
}
