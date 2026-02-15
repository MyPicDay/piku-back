package store.piku.back.recommendation.application.port.in;

import store.piku.back.recommendation.domain.DiaryMetadata;

import java.util.Optional;

/**
 * 일기 콘텐츠 분석 Inbound Port
 */
public interface AnalyzeDiaryContentUseCase {

	DiaryMetadata analyzeAndSave(Long diaryId, String content);

	Optional<DiaryMetadata> getMetadata(Long diaryId);
}
