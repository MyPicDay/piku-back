package com.pikume.back.diary.application.port.out;

import com.pikume.back.creative.application.dto.DiaryImageGenerationView;

public interface LoadCreativePort {
	DiaryImageGenerationView findById(Long id);

	boolean existsByIdAndUserId(Long id, String userId);

	void updateFilePath(Long generationId, String filePath);

	void updateDiaryId(Long generationId, Long diaryId);
}
