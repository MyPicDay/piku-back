package store.piku.back.diary.application.port.out;

import store.piku.back.creative.domain.DiaryImageGeneration;

public interface LoadCreativePort {
	DiaryImageGeneration findById(Long id);

	boolean existsByIdAndUserId(Long id, String userId);

	void updateDiaryId(Long generationId, Long diaryId);
}
